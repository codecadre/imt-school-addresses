## What's this

Portuguese driving schools as available in the [IMT website](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx).

This was originally done for [passaprimeira.xyz](https://www.passaprimeira.xyz).

## Background

[passaprimeira.xyz](https://www.passaprimeira.xyz) aims at bringing transparency to the driving school business in Portugal, by crossing public data and presenting it in an accessible format. Originally it was only a web app, but we decided to release some of the data and code, in a accessible format, so that others could benefit form it.


## Data Origin

[IMT website](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx)

## How to use it

Probably what you want is the output at

- `parsed-data/db.json`
- `parsed-data/db.edn`
- `parsed-data/db.txt` (for debug - easier to look at for a quick reference)

## Unique IDs

Schools have an associated integer value which is unique for the most part. This is the school license number provided by IMT, sometimes is also called "school number", "nec" or "alvará". However, this number appears associated with [multiple schools with different numbers](https://github.com/codecadre/imt-school-addresses/blob/20b1d3a0a4d05c54a906b3c2f55d4ea92ac73d70/duplicates.txt), for multiple reasons, for instance, it might be that a school closed down and the same license was granted to a different school. Because of this, we added a UUID deterministically generated from

```UUID (school name + license nr. + address)```

## Overwrites

In some cases, information on the IMT school profile might not be accurate, for instance, the address might be an old one. Manually fixing these instances is outside the scope of the project, however, in cases where the address has a glaring mistake, we manually fix it with `overwrites.edn`. Here's a few examples:

- [Lago Azul](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LagoAzul%e2%80%93FigueirodosVinhos588.aspx) zip code: `3620` should be `3260`.

## Steps to reproduce this:

### Dependencies

To reproduce the parsing and data processing, you need [`babashka`](https://babashka.org/) and [`nbb`](https://github.com/babashka/nbb).

### Install

```
npm i
```

## Run

```
bb hrefs
```

Runs puppeteer script and produces `hrefs.json` - a collection of links for each school page.

```
bb schools.clj
```

Pulls each school page and produces `schools.edn` with the details.

```
bb cleanup.clj
```

Clean up dataset.

## nrepl

```
bb nrepl
```

for .`clj` files and

```
nbb nrepl-server :port 1337
```

for `*.cljs` files


## Licence (code and open data)

Code is MIT license - basically you can do what you want with the code, just give this project credit for it.

The data being reproduced here is assumed to be in the public domain. Aditionally, when I stated the purpose in the [FOI I filled with IMT](https://www.flaviosousa.co/pedido-accesso-dados-publicos/), no objection was made on the grounds of it being made public.

Acording to the [Open Data Directive](https://digital-strategy.ec.europa.eu/en/policies/open-data), countries are encouraged to make public data accessible, regardless of the end use:

> clearly obliged member states to ‘encourage public sector bodies and public undertakings to produce and make available documents [...] in accordance with the principle of “open by design and by default’’.

[From wikipedia](https://en.wikipedia.org/wiki/Directive_on_the_re-use_of_public_sector_information#Open_data_licensing)

Here, "encouraging" is the key word. Given that no explicit consent or license was given, in principle there's always a chance that this project is using data beyond the scope of it's intended use. We hope to show that initiatives like this bring about positive changes and that they further encourage government bodies to release data with explicit [Open Data licenses](https://en.wikipedia.org/wiki/Directive_on_the_re-use_of_public_sector_information#Open_data_licensing)

## Related projects

https://github.com/codecadre/imt-pass-rates
