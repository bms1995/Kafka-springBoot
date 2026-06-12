create table processed_events (
    order_id varchar(255) primary key
);

create table payment_transactions (
    order_id varchar(255) primary key,
    amount numeric(38, 2),
    status varchar(50),
    customer_email varchar(255),
    failure_reason varchar(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone
);

create table outbox_events (
    event_id varchar(255) primary key,
    aggregate_id varchar(255),
    topic varchar(255),
    event_type varchar(255),
    payload text,
    status varchar(50),
    created_at timestamp(6) with time zone,
    published_at timestamp(6) with time zone
);

create index idx_outbox_events_status_created_at
    on outbox_events (status, created_at);
