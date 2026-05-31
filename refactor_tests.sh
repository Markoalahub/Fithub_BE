#!/bin/bash
cd /Users/myeongsung/Documents/Fithub_BE/src/test/java/markoala/fithub/demo

echo "Refactoring test packages..."

mkdir -p domain
for d in user project issue github pipeline meeting auth; do
    mkdir -p domain/$d
done

for d in user project issue github pipeline meeting auth; do
    find . -name "*.java" -exec sed -i "" "s/package markoala\.fithub\.demo\.$d/package markoala.fithub.demo.domain.$d/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.$d/import markoala.fithub.demo.domain.$d/g" {} +
    find . -name "*.java" -exec sed -i "" "s/markoala\.fithub\.demo\.$d/markoala.fithub.demo.domain.$d/g" {} +
done

for d in user project issue github pipeline meeting auth; do
    if [ -d "$d" ]; then
        git mv $d/* domain/$d/ 2>/dev/null
        rm -rf $d
    fi
done

if [ -d "application" ]; then
    mkdir -p domain/pipeline/controller domain/meeting/controller
    git mv application/controller/Pipeline* domain/pipeline/controller/ 2>/dev/null
    
    find domain/pipeline -name "*.java" -exec sed -i "" "s/package markoala\.fithub\.demo\.application\./package markoala.fithub.demo.domain.pipeline./g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.\([a-z]*\)\.Pipeline/import markoala.fithub.demo.domain.pipeline.\1.Pipeline/g" {} +
    
    rm -rf application
fi

echo "Done!"
