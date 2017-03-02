DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS schooling_level_rankings; 
DROP TABLE IF EXISTS age_groups_rankings; 
DROP TABLE IF EXISTS age_groups;
DROP TABLE IF EXISTS schooling_levels;
DROP TABLE IF EXISTS cities;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS data_imports;
DROP TABLE IF EXISTS users;

CREATE TABLE  cities(
 	id SERIAL PRIMARY KEY,
 	code VARCHAR(6) NOT NULL UNIQUE,
    state CHAR(2) NOT NULL,
    country VARCHAR(25) NOT NULL,
    names VARCHAR(200)[] NOT NULL
);

CREATE TABLE schooling_levels(
	id SERIAL PRIMARY KEY,
    level VARCHAR(180) NOT NULL UNIQUE,
    position SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE age_groups(
	id SERIAL PRIMARY KEY,
    description VARCHAR(180) NOT NULL UNIQUE
);

CREATE TABLE profiles(
	id SERIAL PRIMARY KEY,
    year SMALLINT NOT NULL,
    month SMALLINT,
    sex CHAR(1) NOT NULL,
    electoral_district VARCHAR(10) NOT NULL,
    quantity_peoples INTEGER NOT NULL,
    city_id INTEGER NOT NULL,
    age_group_id INTEGER NOT NULL,
    schooling_level_id INTEGER NOT NULL,
    CONSTRAINT profiles_cities_fk
    	FOREIGN KEY (city_id)
    	REFERENCES cities(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT profiles_age_groups_fk
    	FOREIGN KEY (age_group_id)
    	REFERENCES age_groups(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT profiles_schooling_levels_fk
    	FOREIGN KEY (schooling_level_id)
    	REFERENCES schooling_levels(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE users(
	id SERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
  	active BOOLEAN NOT NULL DEFAULT 't',
  	password VARCHAR(60) NOT NULL
);

CREATE TABLE tasks(
	id SERIAL PRIMARY KEY,
    description VARCHAR(140) NOT NULL,
    user_id INTEGER NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT 'f',
    failure BOOLEAN NOT NULL DEFAULT 'f',
    message VARCHAR(140) NOT NULL,
    CONSTRAINT task_user_fk
    	FOREIGN KEY(user_id)
    	REFERENCES users(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION
);


CREATE TABLE schooling_level_rankings (
	id SERIAL PRIMARY KEY,
    city_code VARCHAR(7) NOT NULL,
    year_or_month VARCHAR(7),
    peoples INTEGER NOT NULL,
    percent_total NUMERIC(10, 6) NOT NULL,
    total INTEGER NOT NULL,
    schooling_level_id INTEGER NOT NULL,
    CONSTRAINT unq_schooling_ranking
        UNIQUE (city_code, year_or_month, schooling_level_id),
    CONSTRAINT fk_schooling_level_id
    	FOREIGN KEY (schooling_level_id)
    	REFERENCES schooling_levels(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION
);


CREATE TABLE age_group_rankings (
	id SERIAL PRIMARY KEY,
    city_code VARCHAR(7) NOT NULL,
    year_or_month VARCHAR(7),
    peoples INTEGER NOT NULL,
    percent_total NUMERIC(10, 6) NOT NULL,
    total INTEGER NOT NULL,
    age_group_id INTEGER NOT NULL,
    CONSTRAINT unq_age_group_ranking
    	UNIQUE (city_code, year_or_month, age_group_id),
    CONSTRAINT fk_age_group_id
    	FOREIGN KEY (age_group_id)
    	REFERENCES age_groups(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE data_imports (
	id SERIAL PRIMARY KEY,
    import_date_time timestamp NOT NULL,
    file_name VARCHAR(150) NOT NULL,
    file_year VARCHAR(4) NOT NULL,
    file_month VARCHAR(2) NOT NULL,
    user_id INTEGER NOT NULL,
    CONSTRAINT data_import_user_fk
    	FOREIGN KEY(user_id)
    	REFERENCES users(id)
    	ON DELETE NO ACTION ON UPDATE NO ACTION

);