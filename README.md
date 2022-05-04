## What's this

Portuguese driving schools as available in the [IMT website](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx).

This was originally done for [passaprimeira.xyz](https://www.passaprimeira.xyz).

## Background

[passaprimeira.xyz](https://www.passaprimeira.xyz) is the second iteration of a project that aims at bringing transparency to the driving school business in Portugal, by crossing public data and presenting it in an accessible format. Originally it was only a web app, but we decided to release some of the data and code, in a usable way, so that others could benefit form it.

## Data Origin

[IMT website](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx)

## How to use it

Probably what you want is the output at

- `parsed-data/db.json`
- `parsed-data/db.edn`
- `parsed-data/db.txt` (for debug but easier to look at for a quick reference)

## Steps to reproduce this:

### Dependencies

To reproduce the parsing and data processing, you need [`babashka`](https://babashka.org/) and [`nbb`](https://github.com/babashka/nbb).

### Install

```
npi i
```

## Run

```
nbb hrefs.cljs
```

Runs puppeteer script and produces `hrefs.json` - a collection of links for each school page.

```
bb schools.clj
```

Pulls each school page and produces `schools.edn` with the details.








The entire data set can be found in



Check [`parse.ipynb`](parse.ipynb).

## Licence

TODO

GPL2 maybe
MIT Maybe




```
nbb scrape.cljs
```

writes `schools.json`.
