

set -e

:: https://rtyley.github.io/bfg-repo-cleaner/
:: https://stackoverflow.com/questions/12450245/getting-a-working-copy-of-a-bare-repository

set project_name=truffler
set git_url=file://e:/project/%project_name%/.git

set BFG=java -jar bfg-1.13.0.jar
set replace_file=strings-to-replace.txt

if exist %replace_file% (

    git clone --mirror %git_url%

    %BFG% --replace-text %replace_file% %project_name%.git

    cd %project_name%.git

    git reflog expire --expire=now --all
    git gc --prune=now --aggressive

    cd ..

    git clone %project_name%.git
) else (
    echo "No strings to replace %replace_file%"
)
