# food-planner

[![CI](https://github.com/NotoBoto/food-planner/actions/workflows/ci.yml/badge.svg)](https://github.com/NotoBoto/food-planner/actions/workflows/ci.yml)

## Lokal starten

API und Ingestion starten nur mit einer echten Kontaktadresse im `User-Agent`:

```powershell
# PowerShell
$env:HTTP_USER_AGENT="FoodPlanner/0.1 (mailto:<deine-echte-adresse>)"
```

```bash
# Bash
export HTTP_USER_AGENT="FoodPlanner/0.1 (mailto:<deine-echte-adresse>)"
```
