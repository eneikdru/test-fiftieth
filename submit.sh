git checkout -b fix/decompose-runtime-contract-fix
git commit -m "Decompose wishlist item for runtime contract fix"
git push origin fix/decompose-runtime-contract-fix
gh pr create --title "Decompose wishlist item for runtime contract fix" --body "Decomposes task 8bd0dbae-41f6-466a-95a7-aff680ed0866 into task plan JSON without adding extra structure to main layer."
