@echo off
setlocal

set file1=result.txt
set file2=result_me.txt
set file3=result_first.txt

fc %file1% %file2% > nul
if %errorlevel% equ 0 (
    echo %file1% and %file2% are identical.
) else (
    echo %file1% and %file2% are different.
)

fc %file1% %file3% > nul
if %errorlevel% equ 0 (
    echo %file1% and %file3% are identical.
) else (
    echo %file1% and %file3% are different.
)

fc %file2% %file3% > nul
if %errorlevel% equ 0 (
    echo %file2% and %file3% are identical.
) else (
    echo %file2% and %file3% are different.
)

endlocal