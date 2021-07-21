# AI Colliseum 2021
## Instructions
1. Follow the AIC installation manual on how to download the game. 
2. After completing those steps, `git clone` this repo into your AIC2021 folder
2. delete the current `src` folder 
3. rename this repo into `src`
    - intelliJ will ask you to reconfigure your git repo. Click the old name of this repo and click the `-` (minus) button to delete it. Then click the new name of the repo, `src`, and click apply to let Intellij know that your git repo is in src.
4. Access your branch via `git checkout` (you may have to `cd` into `src`) and start coding!

## General Git Instructions
1. Always make sure you are in the correct branch! Use `git branch` to see what branch you're on. Use `git checkout <branchname>` to move to the correct branch.
2. Once you have modified some files, and have made a substantial change to your code, you should make a commit. Here is how:
3. `cd` into your git repo and do `git add .`. This will add all modified files in the folder you are in into the staging area.
4. To make sure your modified files are in the staging area, use `git status`. If you modify a file after it is staged, you will have to re-add it to include the new changes.
5. Once all of the modified files that you want are in the staging area, do `git commit -m "change I made"`.  
6. Now you have a version of your project stored in the local repository in your computer. If you want to send all of the commits you saved to the github repoistory, do `git push`.
