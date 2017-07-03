# WEBSITE

## Build the website

We use gulp and hugo to build the harp website. You can refer to [Using Gulp with Hugo](https://devotter.com/blog/using-gulp-with-hugo) for learning about how to use gulp and hugo to build a website.

### Fetch public submodule
website/public is a submodule of harp 
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
gulp clean
gulp build
./scripts/javadocs.sh
hugo
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

