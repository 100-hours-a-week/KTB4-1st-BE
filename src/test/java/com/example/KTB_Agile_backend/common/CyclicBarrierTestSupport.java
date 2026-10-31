package com.example.KTB_Agile_backend.common;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class CyclicBarrierTestSupport {
	private CyclicBarrierTestSupport() {
	}

	public static <T> Pair<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(2);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<T> firstResult = executor.submit(() -> {
				barrier.await(10, TimeUnit.SECONDS);
				return first.call();
			});
			Future<T> secondResult = executor.submit(() -> {
				barrier.await(10, TimeUnit.SECONDS);
				return second.call();
			});
			return new Pair<>(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
		}
	}

	public record Pair<T>(T first, T second) {
	}
}
