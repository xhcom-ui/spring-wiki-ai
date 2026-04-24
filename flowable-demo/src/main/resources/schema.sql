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
    business_key varchar(128),
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
    designer_type varchar(32) default 'BPMN',
    design_schema_json text,
    version integer not null,
    deployment_id varchar(128),
    status varchar(64),
    created_at timestamp,
    updated_at timestamp,
    created_by bigint,
    updated_by bigint
);

alter table if exists process_definition add column if not exists designer_type varchar(32) default 'BPMN';
alter table if exists process_definition add column if not exists design_schema_json text;

create table if not exists form_catalog (
    id bigserial primary key,
    form_key varchar(128) not null unique,
    form_name varchar(255) not null,
    page_label varchar(255),
    component_key varchar(128),
    field_schema_json text,
    scope varchar(32) default 'TASK',
    description varchar(512),
    sort integer default 0,
    status integer default 1,
    created_at timestamp,
    updated_at timestamp
);

alter table if exists form_catalog add column if not exists page_label varchar(255);
alter table if exists form_catalog add column if not exists component_key varchar(128);
alter table if exists form_catalog add column if not exists field_schema_json text;
alter table if exists form_catalog add column if not exists scope varchar(32) default 'TASK';
alter table if exists form_catalog add column if not exists description varchar(512);
alter table if exists form_catalog add column if not exists sort integer default 0;
alter table if exists form_catalog add column if not exists status integer default 1;
alter table if exists form_catalog add column if not exists created_at timestamp;
alter table if exists form_catalog add column if not exists updated_at timestamp;

create index if not exists idx_user_role_role_id on sys_user_role(role_id);
create index if not exists idx_role_menu_menu_id on sys_role_menu(menu_id);
create index if not exists idx_leave_process_instance_id on leave_application(process_instance_id);
create index if not exists idx_leave_business_key on leave_application(business_key);
create index if not exists idx_leave_applicant on leave_application(applicant);
create index if not exists idx_process_definition_key on process_definition(process_key);
create index if not exists idx_form_catalog_status on form_catalog(status);
create index if not exists idx_form_catalog_scope on form_catalog(scope);
