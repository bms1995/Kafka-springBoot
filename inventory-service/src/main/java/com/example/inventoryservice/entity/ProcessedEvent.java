package com.example.inventoryservice.entity;



import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private String orderId;

    public ProcessedEvent(String s) {
        this.orderId = s;
    }

}
