/*
* We found the basis for this solution in
* http://dba.stackexchange.com/questions/42924/postgresql-function-to-create-table
*/
create function create_ranking_table(geo_scope varchar(30)) returns VOID as $$
begin
  execute format(
      'create table if not exists %I (
        packageName character varying(256) not null,
        category varchar(30) not null,
        ranking integer not null,
        primary key (packageName, category)
    )',
    'rankings_' || geo_scope
    );
end
$$ LANGUAGE plpgsql;

select 1 from create_ranking_table('earth') ;

select 1 from create_ranking_table('africa') ;
select 1 from create_ranking_table('americas');
select 1 from create_ranking_table('asia');
select 1 from create_ranking_table('europe');
select 1 from create_ranking_table('oceania');

select 1 from create_ranking_table('country_es');
select 1 from create_ranking_table('country_gb');
select 1 from create_ranking_table('country_us');

drop function if exists create_ranking_table(varchar(30));
