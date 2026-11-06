"""BE dev/prod의 기동 및 헬스 체크 대기와 GHA 취소를 처리한다."""

import json
import os
import subprocess
import sys
import time
from datetime import datetime

SERVICE = os.environ["ECS_SERVICE"]
CLUSTER = os.environ["ECS_CLUSTER"]
TERMINAL = {"Succeeded", "Failed", "Stopped"}


def output(key, value):
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        stream.write(f"{key}={value}\n")


def aws(service, command, *args):
    result = subprocess.run(
        ["aws", service, command, *args, "--output", "json",
         "--cli-connect-timeout", "10", "--cli-read-timeout", "20"],
        check=True, capture_output=True, text=True,
    )
    return json.loads(result.stdout)


def deployment(deployment_id):
    info = aws("deploy", "get-deployment", "--deployment-id", deployment_id)["deploymentInfo"]
    if info["applicationName"] != SERVICE or info["deploymentGroupName"] != SERVICE:
        raise RuntimeError("배포의 애플리케이션 또는 그룹이 서비스와 다릅니다")
    return info


def stop(deployment_id, reason):
    output("reason", reason)
    output("stopped-deployment-id", deployment_id)
    output("stop-status", "중지 요청 실패 또는 미확인")
    aws("deploy", "stop-deployment", "--deployment-id", deployment_id, "--auto-rollback-enabled")
    output("stop-status", "중지 요청 접수")
    print(f"{deployment_id}: {reason}", flush=True)


def cancel():
    deployment_id = os.environ.get("DEPLOYMENT_ID")
    if not deployment_id:
        # Action 출력 전에 취소된 경우에도 이번 실행의 배포만 찾아 중지한다.
        group = aws("deploy", "get-deployment-group", "--application-name", SERVICE,
                    "--deployment-group-name", SERVICE)["deploymentGroupInfo"]
        deployment_id = group.get("lastAttemptedDeployment", {}).get("deploymentId")
    if not deployment_id:
        output("reason", "이번 실행에서 중지할 AWS 배포가 없음")
        return
    info = deployment(deployment_id)
    if info.get("description", "").rsplit(" / ", 1)[-1] != os.environ["GITHUB_RUN_ID"]:
        output("reason", "이번 실행이 생성한 AWS 배포가 아니므로 중지 생략")
        return
    if info["status"] in TERMINAL:
        output("reason", "이번 실행의 AWS 배포가 이미 종료돼 중지 생략")
        return
    stop(deployment_id, "GHA 실행 취소로 AWS 배포 중지 및 자동 롤백 요청")


def monitor():
    deployment_id = os.environ["DEPLOYMENT_ID"]
    minutes = int(os.environ["DEPLOY_STARTUP_MINUTES"])
    info = deployment(deployment_id)
    deadline = datetime.fromisoformat(info["createTime"].replace("Z", "+00:00")).timestamp() + minutes * 60
    healthy = False
    while True:
        print(f"{deployment_id}: {info['status']}", flush=True)
        if info["status"] == "Succeeded":
            return
        if info["status"] in {"Failed", "Stopped"}:
            raise RuntimeError(f"AWS 배포가 {info['status']} 상태로 종료됐음")
        if not healthy:
            try:
                target = aws("deploy", "get-deployment-target", "--deployment-id", deployment_id,
                             "--target-id", f"{CLUSTER}:{SERVICE}")["deploymentTarget"]["ecsTarget"]
            except subprocess.CalledProcessError as error:
                if not any(code in error.stderr for code in (
                        "DeploymentNotStartedException", "DeploymentTargetDoesNotExistException")):
                    raise
                target = {}
            healthy = any(event["lifecycleEventName"] == "AllowTraffic" and event["status"] == "Succeeded"
                          for event in target.get("lifecycleEvents", []))
            if not healthy and time.time() >= deadline:
                # 제한 시각에 완료된 배포는 중지하지 않는다.
                info = deployment(deployment_id)
                if info["status"] in TERMINAL:
                    continue
                reason = f"신규 태스크 기동 및 헬스 체크 {minutes}분 제한 초과"
                stop(deployment_id, reason)
                raise RuntimeError(reason)
        # 트래픽 전환 후에는 기존 구버전 유지 시간이 끝나기를 기다린다.
        time.sleep(15)
        info = deployment(deployment_id)


if __name__ == "__main__":
    try:
        if os.environ["DEPLOYMENT_MODE"] == "cancel":
            cancel()
        else:
            monitor()
    except (RuntimeError, subprocess.CalledProcessError, KeyError, ValueError) as error:
        message = error.stderr.strip() if isinstance(error, subprocess.CalledProcessError) else str(error)
        output("error", "AWS API 요청 실패. 배포 상태 또는 중지 요청 확인이 필요함"
               if isinstance(error, subprocess.CalledProcessError)
               else "배포 처리 실패. GitHub Actions 실행 로그에서 확인해야 함")
        print(f"::error::{message}", flush=True)
        sys.exit(1)
