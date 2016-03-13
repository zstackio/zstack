Contributing to ZStack
=======================================

Summary
-------
This document covers how to contribute to the ZStack project. These instructions assume you have a GitHub.com account, please create one if you don't have. Your proposed code changes will be published to your own fork of the ZStack project, and you need to submit a Pull Request for your changes to be added.

_Lets get started!!!_


Fork the code 
-------------

In your browser, navigate to: [https://github.com/zstackorg/zstack](https://github.com/zstackorg/zstack)

Fork the repository by clicking the 'Fork' button on the top right.  After the fork completes, the page will be redirected to the forked repository.

Please follow below steps to setup a local ZStack repository:

``` bash
$ git clone https://github.com/YOUR_ACCOUNT/zstack.git (you can find the URL on the page of the forked repository)
$ cd zstack
$ git remote add upstream https://github.com/zstackorg/zstack.git
$ git checkout master
$ git fetch upstream
$ git rebase upstream/master
```


Making changes
--------------

You need to create a new branch to make changes, and do not directly change the `master` branch.  In the following example, We assume you are going to make changes to a branch `feature_x`, which is created in your local repository and will be pushed to the remote forked repository later.  Once the branch is pushed, you can create a Pull Request to the ZStack project.

The best practice is to create a new branch each time you want to contribute a patch and only track the changes for that pull request in the branch.

``` bash
$ git checkout -b feature_x
   (make your changes)
$ git status
$ git add .
$ git commit -a -m "descriptive commit message for your changes"
```

> The `-b` is for creating the new branch `feature_x`; it's used the first time you create a new branch.


Rebase `feature_x` to include updates from `upstream/master`
------------------------------------------------------------

It's important to use `git rebase` to keep an up-to-date `master` branch in your local repository. You need to do this before starting to work on a new feature or making a pull request.

This process is like:

1. Check out to your local `master` branch
2. Synchronize the local `master` branch with the `upstream/master` so it has the latest changes
3. Rebase the latest changes into the `feature_x` branch to make it up-to-date

``` bash
$ git checkout master
$ git fetch upstream
$ git rebase upstream/master
$ git checkout feature_x
$ git rebase master
```

> Now your `feature_x` branch is up-to-date with `upstream/master`.


Make a GitHub Pull Request
--------------------------

Now you are ready to make a pull request. This is done by pushing your local changes to your remote forked repository (default remote name is `origin`) and then initiating a pull request on GitHub.

> **IMPORTANT:** Make sure you have followed the above chapter to make the `feature_x` branch up-to-date.

``` bash
$ git push origin master
$ git push origin feature_x
```

To initiate the pull request, do following:

1. Open your forked repository: [https://github.com/YOUR_ACCOUNT/zstack](https://github.com/YOUR_ACCOUNT/zstack)
2. Click the new button '**Compare & pull request**'
3. Validate the destination is `master` branch of ZStack and the source branch is your `feature_x` branch
4. Enter a detailed description and click the button '**Send pull request**'

If you are requested to make modifications to your proposed changes, make the changes locally on your `feature_x` branch, re-push the `feature_x` branch to your fork. The existing pull request should automatically pick up the change.


Cleaning up after a successful pull request
-------------------------------------------

Once the `feature_x` branch has been committed into the `upstream/master` branch, your local `feature_x` branch and the `origin/feature_x` branch are no longer needed.  If you want to make additional changes, restart the process with a new branch.

> **IMPORTANT:** Make sure that your changes are in `upstream/master` before you delete your `feature_x` and `origin/feature_x` branches!

You can delete these deprecated branches with the following:

``` bash
$ git checkout master
$ git branch -D feature_x
$ git push origin :feature_x
```
