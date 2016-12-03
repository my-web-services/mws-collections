#!/bin/bash
find ./app -type f -name '*.coffee' -not -name 'app.coffee' -print0 | xargs -0 rm --
find ./app/ -type d -name 'views' -not -wholename './app/accounts/views' -not -wholename './app/main/views' | xargs rm -rf
sed -n '1,/Javascripts/p;/app.js/,$p' app/main/views/_template.scala.html > tmp && mv tmp app/main/views/_template.scala.html
sed -n '1,/ul/p;/ul>/,$p' app/main/views/mainIndex.scala.html > tmp && mv tmp app/main/views/mainIndex.scala.html
sed -n '1,/confirm/p;/\]/,$p' app/assets/javascripts/app.coffee > tmp && mv tmp app/assets/javascripts/app.coffee
sed -n '1,/INSERT INTO \"account\" /p;/Downs/,$p' conf/evolutions/default/1.sql > tmp && mv tmp conf/evolutions/default/1.sql
sed -n '1,/Downs/p;/DROP TABLE \"account\"/,$p' conf/evolutions/default/1.sql > tmp && mv tmp conf/evolutions/default/1.sql
sed -n '1,/GET.*webjars.*/p;/%/,$p' conf/routes > tmp && mv tmp conf/routes
find ./macros_project/macros/ -type d -name 'state' | xargs rm -rf
