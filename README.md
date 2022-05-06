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

## unique ids and data issues

Schools have an integer number which is unique for the most part. IMT calls this the "school number", "nec" or "alvará".

However, given that this number appears associated with [multiple schools with different numbers](https://github.com/codecadre/imt-school-addresses/blob/20b1d3a0a4d05c54a906b3c2f55d4ea92ac73d70/duplicates.txt), etc, we added a UUID deterministically generated from the url of each school, which by definition, is a unique record.


## Steps to reproduce this:

### Dependencies

To reproduce the parsing and data processing, you need [`babashka`](https://babashka.org/) and [`nbb`](https://github.com/babashka/nbb).

### Install

```
npm i
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

## nrepl

```
bb nrepl
```

for .`clj` files and

```
nbb nrepl-server :port 1337
```

for `*.cljs` files


## Licence

TODO

GPL2 maybe
MIT Maybe
