create table if not exists sys_user (
    id bigserial primary key,
    username varchar(128) not null unique,
    password varchar(255) not null,
    nickname varchar(128),
    email varchar(255),
    phone varchar(64),
    status integer,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists sys_role (
    id bigserial primary key,
    name varchar(128) not null,
    code varchar(128) not null unique,
    description varchar(512),
    status integer,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists sys_menu (
    id bigserial primary key,
    name varchar(128) not null,
    path varchar(255) not null unique,
    component varchar(255),
    icon varchar(128),
    parent_id bigint default 0,
    sort integer default 0,
    type integer default 1,
    permission varchar(255),
    status integer,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists sys_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);

create table if not exists sys_role_menu (
    role_id bigint not null,
    menu_id bigint not null,
    primary key (role_id, menu_id)
);

create table if not exists leave_application (
    id bigserial primary key,
    process_instance_id varchar(128),
    applicant varchar(128),
    dept_manager varchar(128),
    general_manager varchar(128),
    status varchar(64),
    start_date timestamp,
    end_date timestamp,
    days integer,
    reason text,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists process_definition (
    id bigserial primary key,
    process_key varchar(128) not null,
    process_name varchar(255) not null,
    bpmn_xml text not null,
    designer_type varchar(64),
    designer_json text,
    version integer not null,
    deployment_id varchar(128),
    status varchar(64),
    created_at timestamp,
    updated_at timestamp,
    created_by bigint,
    updated_by bigint
);

create table if not exists task_page_catalog (
    id bigserial primary key,
    item_key varchar(128) not null unique,
    label varchar(255) not null,
    item_type varchar(32) not null,
    page_mode varchar(16),
    business_kind varchar(64),
    description varchar(512),
    default_todo_page varchar(128),
    default_done_page varchar(128),
    system_flag integer default 0,
    sort integer default 0,
    status integer default 1,
    created_at timestamp,
    updated_at timestamp
);

create index if not exists idx_user_role_role_id on sys_user_role(role_id);
create index if not exists idx_role_menu_menu_id on sys_role_menu(menu_id);
create index if not exists idx_leave_process_instance_id on leave_application(process_instance_id);
create index if not exists idx_leave_applicant on leave_application(applicant);
create index if not exists idx_process_definition_key on process_definition(process_key);
create index if not exists idx_task_page_catalog_type on task_page_catalog(item_type, page_mode, status);

alter table process_definition add column if not exists designer_type varchar(64);
alter table process_definition add column if not exists designer_json text;
