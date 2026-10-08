package com.example.KTB_Agile_backend.item.event;

@FunctionalInterface
public interface ItemViewEventPublisher {

	void publish(ItemViewEvent event);
}
