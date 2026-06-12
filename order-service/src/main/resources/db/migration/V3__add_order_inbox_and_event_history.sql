create table processed_events (
    event_id varchar(255) primary key,
    order_id varchar(255),
    event_type varchar(255),
    processed_at timestamp(6) with time zone
);

create table order_event_history (
    history_id varchar(255) primary key,
    event_id varchar(255),
    order_id varchar(255),
    event_type varchar(255),
    source_topic varchar(255),
    payload text,
    received_at timestamp(6) with time zone
);

create index idx_order_event_history_order_received_at
    on order_event_history (order_id, received_at);
