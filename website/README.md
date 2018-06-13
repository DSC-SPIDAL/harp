# WEBSITE

## Build the website

We use gulp and hugo to build the harp website. You can refer to [Using Gulp with Hugo](https://devotter.com/blog/using-gulp-with-hugo) for learning about how to use gulp and hugo to build a website.

### Fetch public submodule
website/public is a submodule of harp repository 
If you already use option "--recursive" (supported by git version 1.9 and later) when you git clone 
harp repository, the website/public as a submodule has already been downloaded. Otherwise, you shall manually
fetch the submodule source code of website/public as follows.
```bash
# enter the root directory of harp
cd harp/ 
# retrieve all the submodule
git submodule update --init --recursive 
# enter the public directory
cd website/public
# checkout to gh-pages
git checkout gh-pages
# rename remote
git remote rename origin upstream
```

### Install required packages. You only need to do it for the first time.
```bash
cd website
npm install
```

### Build website
```bash
cd website
gulp clean
#run 'npm rebuild node-sass' if got error message
gulp build
./scripts/javadocs.sh
hugo
#restore the first page with new designed style
cp design/index.html public
```

### View the website locally
```bash
hugo server --watch --ignoreCache
```

### Submit changes back to harp repository
```bash
cd website/public
git add .
git commit -a
git push upstream gh-pages
```

