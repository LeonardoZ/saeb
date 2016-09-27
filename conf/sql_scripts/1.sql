create database if not exists saeb;
use saeb;

create table user (
  id int(11) not null auto_increment primary key ,
  email varchar(120) not null,
  remember tinyint(1) default '0',
  password varchar(60) not null,
  unique key unq_email (email)
)

create table if not exists task (
	id int primary key auto_increment,
    description varchar(140) not null,
    user_id int not null,
    completed boolean not null default false,
    failure boolean not null default false,
    message varchar(140) not null,
    foreign key (user_id) references user(id)
);

create table if not exists age_group (
	id int primary key auto_increment,
    group_description varchar(30) not null unique
);

create table if not exists  schooling (
	id int primary key auto_increment,
	level varchar(30) not null unique
);

create table if not exists city (
	id int primary key auto_increment,
	name varchar(45) not null unique,
    city_code varchar(7) not null,
    state varchar(2) not null,
    country varchar(35) not null
);

create table if not exists  profile (
	id int primary key auto_increment,
	year_or_month varchar(7) not null,
    electoral_district varchar(6) not null,
    sex varchar(13) not null,
    quantity_of_peoples int not null,
    city_id int not null,
    age_group_id int not null,
    schooling_id int not null,
    foreign key fk_city_id (city_id) references city(id),
    foreign key fk_age_group_id (age_group_id) references age_group(id),
    foreign key fk_schooling_id (schooling_id) references schooling(id)
);

create table if not exists data_import(
	id int primary key not null auto_increment,
    import_date_time datetime not null,
    file_name varchar(150) not null,
    file_year varchar(4) not null,
    file_month varchar(2)
);


create table if not exists user (
  id int(11) primary key auto_increment,
  email varchar(120) not null,
  remember tinyint(1) default 0,
  password varchar(60) not null,
  unique key unq_email(email)
);



