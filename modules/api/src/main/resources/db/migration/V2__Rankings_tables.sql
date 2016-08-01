/*
* We found the basis for this solution in
* http://dba.stackexchange.com/questions/42924/postgresql-function-to-create-table
*/
create or replace function create_ranking_table(geo_scope varchar(30)) returns VOID as $$
begin
  execute format(
    'create table if not exists %I (
        packageName character varying(256) not null,
        category varchar(30) not null,
        ranking integer not null,
        primary key (packageName, category)
    )',
    'Rankings_' || geo_scope
    );
end
$$ LANGUAGE plpgsql;

select 1 from create_ranking_table('Earth') ;

select 1 from create_ranking_table('Africa') ;
select 1 from create_ranking_table('Americas');
select 1 from create_ranking_table('Asia');
select 1 from create_ranking_table('Europe');
select 1 from create_ranking_table('Oceania');

select 1 from create_ranking_table('Country_ES');
select 1 from create_ranking_table('Country_GB');
select 1 from create_ranking_table('Country_US');

drop function if exists create_ranking_table(varchar(30));
