DROP TABLE IF EXISTS users;

create sequence  IF NOT EXISTS users_seq;

CREATE TABLE IF NOT EXISTS users (
  id bigint default users_seq.nextval primary key,
  user_id char(30),
  email varchar(256) NOT NULL,
  mobile char(11),
  password char(128),
  email_verified number(1),
  name varchar(64),
  picture_url varchar(256),
  locale char(2),
  family_name varchar(64),
  given_name varchar(64)
);

INSERT INTO users(id, email, password) select * from (
select 1, 'admin@gmail.com', '1234'
) x where not exists(select * from users);
