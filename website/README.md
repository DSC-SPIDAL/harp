
# Build website

under 'website' directory:

npm install

gulp clean

gulp build

./scripts/javadocs.sh 

hugo

hugo server --watch --ignoreCache

Note: please refer to https://devotter.com/blog/using-gulp-with-hugo for learning about how to use gulp and hugo to build a website.

# use gh-pages branch as a submodule

website/public is a submodule pointing to gh-pages branch.
one time setup: 
rm -rf public

git submodule add https://github.com/DSC-SPIDAL/harp.git public

git submodule update --init

cd public

git checkout gh-pages

git remote rename origin upstream
