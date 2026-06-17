alter table outbox_events
    add column processing_started_at timestamp(6) with time zone;

create index idx_order_outbox_events_processing_started_at
    on outbox_events (status, processing_started_at);
