alter table processed_events
    add column event_id varchar(255);

update processed_events
set event_id = order_id
where event_id is null;

alter table processed_events
    alter column event_id set not null,
    alter column order_id set not null;

alter table processed_events
    drop constraint processed_events_pkey,
    add constraint processed_events_pkey primary key (event_id);

create index idx_inventory_processed_events_order_id
    on processed_events (order_id);
