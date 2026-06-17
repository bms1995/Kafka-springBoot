alter table outbox_events
    add column attempt_count integer not null default 0,
    add column last_error text;
