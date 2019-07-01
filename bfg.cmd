

set BFG=E:/dump/java/bfg-1.13.0.jar

%BFG% --delete-files truffit-report.xml
%BFG% --delete-files truffit-report.txt

git reflog expire --expire=now --all && git gc --prune=now --aggressive

