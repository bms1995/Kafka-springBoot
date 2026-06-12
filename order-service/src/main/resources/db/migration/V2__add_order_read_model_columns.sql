alter table orders
    add column failure_reason varchar(255),
    add column updated_at timestamp(6) with time zone;

update orders
set updated_at = created_at
where updated_at is null;
