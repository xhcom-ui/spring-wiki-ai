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

create table if not exists sys_department (
    id bigserial primary key,
    name varchar(128) not null,
    code varchar(128) not null unique,
    leader varchar(128),
    parent_id bigint default 0,
    status integer,
    description varchar(512),
    created_at timestamp,
    updated_at timestamp
);

create table if not exists sys_post (
    id bigserial primary key,
    name varchar(128) not null,
    code varchar(128) not null unique,
    department_id bigint not null,
    level varchar(64),
    status integer,
    description varchar(512),
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

create table if not exists sys_form_catalog (
    id bigserial primary key,
    form_key varchar(128) not null unique,
    title varchar(255) not null,
    description varchar(1000),
    schema_json text not null,
    status integer,
    created_at timestamp,
    updated_at timestamp
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
    passed_node_labels_json text,
    created_at timestamp,
    updated_at timestamp
);

alter table leave_application add column if not exists passed_node_labels_json text;

create table if not exists process_definition (
    id bigserial primary key,
    process_key varchar(128) not null,
    process_name varchar(255) not null,
    bpmn_xml text not null,
    designer_type varchar(32) default 'BPMN',
    design_schema_json text,
    version integer not null,
    status varchar(64),
    created_at timestamp,
    updated_at timestamp,
    created_by bigint,
    updated_by bigint
);

alter table process_definition add column if not exists designer_type varchar(32) default 'BPMN';
alter table process_definition add column if not exists design_schema_json text;

create table if not exists flowlong_deployment_record (
    id bigserial primary key,
    process_key varchar(128) not null,
    process_name varchar(255),
    flow_long_process_id bigint,
    flow_long_process_version integer,
    source_definition_id bigint,
    source_definition_version integer,
    source_type varchar(64),
    deployed_by bigint,
    deployed_by_name varchar(128),
    deployed_at timestamp
);

create table if not exists process_instance_audit_log (
    id bigserial primary key,
    process_key varchar(128),
    process_instance_id varchar(128) not null,
    business_key varchar(128),
    task_id bigint,
    task_key varchar(128),
    task_name varchar(255),
    node_label varchar(255),
    page_label varchar(255),
    form_key varchar(128),
    form_label varchar(255),
    action_type varchar(64),
    comment text,
    approval_comment text,
    reject_reason text,
    system_remark text,
    variable_snapshot_json text,
    form_snapshot_json text,
    operator_id bigint,
    operator_name varchar(128),
    created_at timestamp
);

alter table process_instance_audit_log add column if not exists approval_comment text;
alter table process_instance_audit_log add column if not exists reject_reason text;
alter table process_instance_audit_log add column if not exists system_remark text;

create table if not exists flw_process (
    id bigint primary key,
    tenant_id varchar(50),
    create_id varchar(50) not null,
    create_by varchar(50) not null,
    create_time timestamp not null,
    process_key varchar(100) not null,
    process_name varchar(100) not null,
    process_icon varchar(255),
    process_type varchar(100),
    process_version integer not null default 1,
    instance_url varchar(200),
    remark varchar(255),
    use_scope smallint not null default 0,
    process_state smallint not null default 1,
    model_content text,
    sort smallint
);

create table if not exists flw_instance (
    id bigint primary key,
    tenant_id varchar(50),
    create_id varchar(50) not null,
    create_by varchar(50) not null,
    create_time timestamp not null,
    process_id bigint not null,
    parent_instance_id bigint,
    priority smallint,
    instance_no varchar(50),
    business_key varchar(100),
    variable text,
    current_node_name varchar(100) not null,
    current_node_key varchar(100) not null,
    expire_time timestamp,
    last_update_by varchar(50),
    last_update_time timestamp
);

create table if not exists flw_his_instance (
    id bigint primary key,
    tenant_id varchar(50),
    create_id varchar(50) not null,
    create_by varchar(50) not null,
    create_time timestamp not null,
    process_id bigint not null,
    parent_instance_id bigint,
    priority smallint,
    instance_no varchar(50),
    business_key varchar(100),
    variable text,
    current_node_name varchar(100) not null,
    current_node_key varchar(100) not null,
    expire_time timestamp,
    last_update_by varchar(50),
    last_update_time timestamp,
    instance_state smallint not null default 0,
    end_time timestamp,
    duration bigint
);

create table if not exists flw_task (
    id bigint primary key,
    tenant_id varchar(50),
    create_id varchar(50) not null,
    create_by varchar(50) not null,
    create_time timestamp not null,
    instance_id bigint not null,
    parent_task_id bigint,
    task_name varchar(100) not null,
    task_key varchar(100) not null,
    task_type smallint not null,
    perform_type smallint,
    action_url varchar(200),
    variable text,
    assignor_id varchar(100),
    assignor varchar(255),
    expire_time timestamp,
    remind_time timestamp,
    remind_repeat smallint not null default 0,
    viewed smallint not null default 0
);

create table if not exists flw_his_task (
    id bigint primary key,
    tenant_id varchar(50),
    create_id varchar(50) not null,
    create_by varchar(50) not null,
    create_time timestamp not null,
    instance_id bigint not null,
    parent_task_id bigint,
    call_process_id bigint,
    call_instance_id bigint,
    task_name varchar(100) not null,
    task_key varchar(100) not null,
    task_type smallint not null,
    perform_type smallint,
    action_url varchar(200),
    variable text,
    assignor_id varchar(100),
    assignor varchar(255),
    expire_time timestamp,
    remind_time timestamp,
    remind_repeat smallint not null default 0,
    viewed smallint not null default 0,
    finish_time timestamp,
    task_state smallint not null default 0,
    duration bigint
);

create table if not exists flw_task_actor (
    id bigint primary key,
    tenant_id varchar(50),
    instance_id bigint not null,
    task_id bigint not null,
    actor_id varchar(100) not null,
    actor_name varchar(100) not null,
    actor_type integer not null,
    weight integer,
    agent_id varchar(100),
    agent_type integer,
    ext text
);

create table if not exists flw_his_task_actor (
    id bigint primary key,
    tenant_id varchar(50),
    instance_id bigint not null,
    task_id bigint not null,
    actor_id varchar(100) not null,
    actor_name varchar(100) not null,
    actor_type integer not null,
    weight integer,
    agent_id varchar(100),
    agent_type integer,
    ext text
);

create table if not exists flw_ext_instance (
    id bigint primary key,
    tenant_id varchar(50),
    process_id bigint not null,
    process_name varchar(100),
    process_type varchar(100),
    model_content text
);

create index if not exists idx_user_role_role_id on sys_user_role(role_id);
create index if not exists idx_role_menu_menu_id on sys_role_menu(menu_id);
create index if not exists idx_form_catalog_status on sys_form_catalog(status);
create index if not exists idx_leave_process_instance_id on leave_application(process_instance_id);
create index if not exists idx_leave_business_key on leave_application(business_key);
create index if not exists idx_leave_applicant on leave_application(applicant);
create index if not exists idx_process_definition_key on process_definition(process_key);
create index if not exists idx_flowlong_deploy_process_key on flowlong_deployment_record(process_key);
create index if not exists idx_flowlong_deploy_time on flowlong_deployment_record(deployed_at);
create index if not exists idx_audit_instance on process_instance_audit_log(process_instance_id);
create index if not exists idx_audit_created on process_instance_audit_log(created_at);
create index if not exists idx_flw_process_key on flw_process(process_key);
create index if not exists idx_flw_process_name on flw_process(process_name);
create index if not exists idx_flw_instance_process_id on flw_instance(process_id);
create index if not exists idx_flw_his_instance_process_id on flw_his_instance(process_id);
create index if not exists idx_flw_task_instance_id on flw_task(instance_id);
create index if not exists idx_flw_task_parent_task_id on flw_task(parent_task_id);
create index if not exists idx_flw_his_task_instance_id on flw_his_task(instance_id);
create index if not exists idx_flw_his_task_parent_task_id on flw_his_task(parent_task_id);
create index if not exists idx_flw_task_actor_task_id on flw_task_actor(task_id);
create index if not exists idx_flw_his_task_actor_task_id on flw_his_task_actor(task_id);
