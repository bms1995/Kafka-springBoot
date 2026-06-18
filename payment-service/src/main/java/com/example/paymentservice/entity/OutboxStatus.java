package com.example.paymentservice.entity;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  PUBLISHED,
  DEAD
}
