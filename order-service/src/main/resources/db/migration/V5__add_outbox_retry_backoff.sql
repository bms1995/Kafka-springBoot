alter table outbox_events
    add column next_attempt_at timestamp(6) with time zone;

update outbox_events
set next_attempt_at = created_at
where status = 'PENDING' and next_attempt_at is null;

create index idx_order_outbox_events_status_next_attempt_at_created_at
    on outbox_events (status, next_attempt_at, created_at);
