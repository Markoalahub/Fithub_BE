#!/bin/bash

# Main source
cd /Users/myeongsung/Documents/Fithub_BE/src/main/java/markoala/fithub/demo

echo "Refactoring main packages..."

mkdir -p domain
for d in user project issue github pipeline meeting auth; do
    mkdir -p domain/$d
done

# 1. Update references in code
for d in user project issue github pipeline meeting auth; do
    find . -name "*.java" -exec sed -i "" "s/package markoala\.fithub\.demo\.$d/package markoala.fithub.demo.domain.$d/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.$d/import markoala.fithub.demo.domain.$d/g" {} +
    find . -name "*.java" -exec sed -i "" "s/markoala\.fithub\.demo\.$d/markoala.fithub.demo.domain.$d/g" {} +
done

# 2. Move directories
for d in user project issue github pipeline meeting auth; do
    if [ -d "$d" ]; then
        git mv $d/* domain/$d/
        rm -rf $d
    fi
done

# 3. application -> pipeline/meeting
mkdir -p domain/pipeline/client domain/pipeline/controller domain/pipeline/service domain/pipeline/dto/request domain/pipeline/dto/response
mkdir -p domain/meeting/client domain/meeting/controller domain/meeting/service domain/meeting/dto/request domain/meeting/dto/response

if [ -d "application" ]; then
    git mv application/client/Pipeline* domain/pipeline/client/ 2>/dev/null
    git mv application/controller/Pipeline* domain/pipeline/controller/ 2>/dev/null
    git mv application/service/Pipeline* domain/pipeline/service/ 2>/dev/null
    git mv application/dto/request/Pipeline* domain/pipeline/dto/request/ 2>/dev/null
    git mv application/dto/response/Pipeline* domain/pipeline/dto/response/ 2>/dev/null
    git mv application/dto/response/FeatResponse* domain/pipeline/dto/response/ 2>/dev/null
    git mv application/dto/response/ProjectPipeline* domain/pipeline/dto/response/ 2>/dev/null

    git mv application/dto/request/Meeting* domain/meeting/dto/request/ 2>/dev/null
    git mv application/dto/response/Meeting* domain/meeting/dto/response/ 2>/dev/null
    
    # Fix package definitions in moved application files
    find domain/pipeline -name "*.java" -exec sed -i "" "s/package markoala\.fithub\.demo\.application\./package markoala.fithub.demo.domain.pipeline./g" {} +
    find domain/meeting -name "*.java" -exec sed -i "" "s/package markoala\.fithub\.demo\.application\./package markoala.fithub.demo.domain.meeting./g" {} +

    # Global import fixes
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.\([a-z]*\)\.Pipeline/import markoala.fithub.demo.domain.pipeline.\1.Pipeline/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.request\.Pipeline/import markoala.fithub.demo.domain.pipeline.dto.request.Pipeline/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.response\.Pipeline/import markoala.fithub.demo.domain.pipeline.dto.response.Pipeline/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.response\.FeatResponse/import markoala.fithub.demo.domain.pipeline.dto.response.FeatResponse/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.response\.ProjectPipeline/import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipeline/g" {} +

    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.request\.Meeting/import markoala.fithub.demo.domain.meeting.dto.request.Meeting/g" {} +
    find . -name "*.java" -exec sed -i "" "s/import markoala\.fithub\.demo\.application\.dto\.response\.Meeting/import markoala.fithub.demo.domain.meeting.dto.response.Meeting/g" {} +

    find . -name "*.java" -exec sed -i "" "s/markoala\.fithub\.demo\.application\.\([a-z]*\)\.Pipeline/markoala.fithub.demo.domain.pipeline.\1.Pipeline/g" {} +
    find . -name "*.java" -exec sed -i "" "s/markoala\.fithub\.demo\.application\.dto\.response\.ProjectPipeline/markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipeline/g" {} +

    rm -rf application
fi

echo "Refactoring test packages..."
# Tests
cd ../../../../../src/test/java/markoala/fithub/demo

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
