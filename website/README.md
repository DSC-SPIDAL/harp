# WEBSITE

## Build the website

We use gulp and hugo to build the harp website. You can refer to [Using Gulp with Hugo](https://devotter.com/blog/using-gulp-with-hugo) for learning about how to use gulp and hugo to build a website.

#### Install required packages. You only need to do it for the first time.
    cd website
    npm install

#### Build website
    gulp clean
    gulp build
    ./scripts/javadocs.sh
    hugo

#### View the website locally
    hugo server --watch --ignoreCache


## Set gh-pages branch as a submodule
####We set website/public as a submodule pointing to gh-pages branch. It's one time setup.

    rm -rf public
    git submodule add https://github.com/DSC-SPIDAL/harp.git public
    git submodule update --init
    cd public
    git checkout gh-pages
    git remote rename origin upstream

