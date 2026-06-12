create table orders (
    order_id varchar(255) primary key,
    product_name varchar(255),
    quantity integer not null,
    amount numeric(38, 2),
    customer_email varchar(255),
    status varchar(50),
    created_at timestamp(6) with time zone
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

create index idx_order_outbox_events_status_created_at
    on outbox_events (status, created_at);
