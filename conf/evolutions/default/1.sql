# --- !Ups
CREATE TABLE "account" (
  "name" varchar(200) PRIMARY KEY,
  "password" varchar(200)
);
INSERT INTO "account" values ('orestis', '1234');
CREATE TABLE "book" (
   "title" VARCHAR PRIMARY KEY,
   "author" VARCHAR,
   "year" INTEGER,
   "accountId" VARCHAR
);
INSERT INTO "book" values ('dummy', 'dummy', 666, 'orestis');

CREATE TABLE "conference" (
   "title" VARCHAR PRIMARY KEY,
   "period" VARCHAR,
   "link" VARCHAR,
   "accountId" VARCHAR
);
INSERT INTO "conference" values ('dummy', 'dummy', 'dummy', 'orestis');

CREATE TABLE "paper" (
   "title" VARCHAR PRIMARY KEY,
   "author" VARCHAR,
   "domain" VARCHAR,
   "link" VARCHAR,
   "accountId" VARCHAR
);
INSERT INTO "paper" values ('dummy', 'dummy', 'dummy', 'dummy', 'orestis');

CREATE TABLE "quote" (
   "quote" VARCHAR PRIMARY KEY,
   "author" VARCHAR,
   "category" VARCHAR,
   "accountId" VARCHAR
);
INSERT INTO "quote" values ('dummy', 'dummy', 'dummy', 'orestis');

CREATE TABLE "talk" (
   "title" VARCHAR PRIMARY KEY,
   "speaker" VARCHAR,
   "link" VARCHAR,
   "accountId" VARCHAR
);
INSERT INTO "talk" values ('dummy', 'dummy', 'dummy', 'orestis');

# --- !Downs

DROP TABLE "talk";

DROP TABLE "quote";

DROP TABLE "paper";

DROP TABLE "conference";

DROP TABLE "book";
DROP TABLE "account";