## What's this?

This repository mirrors the driving schools available at [imt-ip.pt](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx). It's a collection of scripts that scrape the website and respective school files.

The goal is to have a 1-1 mapping between each school URL and a file under `/parsed-data`.

For example, `parsed-data/castelo-branco/fundao/035c3e16-a-cereja.edn` looks like:

```clojure
{:address
 "Loteamento Quinta do Espírito Santo, lote 10 r/c Frente 6230-329 FUNDÃO",
 :name "A Cereja",
 :href-id #uuid "035c3e16-af4a-38d1-9233-2173e6767f72",
 :distrito "Castelo Branco",
 :imt-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/ACereja005.aspx",
 :concelho "Fundão",
 :nec 668,
 :cp7 "6230-329",
 :id #uuid "2ab49907-1192-3122-9793-687bc6136515",
 :concelho-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=CasteloBranco&Concelho=Fund%C3%A3o"}
```

Which mirrors the URL under `imt-href`:
https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/ACereja005.aspx

### Filepath structure vs imt-ip.pt URL

The file path looks like this:

```
/parsed-data/[:district]/[:municipality]/[:digest]-[:name].edn
```

`:district`, `:municipality`, and `:name` are self-explanatory.

#### `:digest`

From the user's point of view, schools are organized geographically: districts > county/municipality (_concelhos_) > schools. If you click on [imt-ip.pt](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx), you're shown a map of the country, then you have to click on the district, municipality, and then you're shown a list of schools.

The URL of each school has a unique portion, in the example above it's `ACereja005`. This is loosely associated with the school `name` and license number `nec`; however, the rules are a bit fuzzy. Instead, what we did was digest the URL (UUID v3 with an MD5 algorithm) and take the first eight characters. In the example above, that is `035c3e16`.

### Inserts vs updates: what constitutes a "school entry"?

There are no guarantees that each URL corresponds to one school. It's possible that the `name` and `nec` can change for the same URL. To keep our data model consistent, we've appended the name to the file path. Each name change corresponds to a new school effectively.

So, for instance, in the example above, if that school name is updated to `:name "Mega School"`, the new filename becomes `035c3e16-mega-school.edn`. **All other attribute changes are considered updates rather than inserts**.

### Archive

With each new data fetch, we check which files weren't updated and add a key representing the time of the last fetch. For example:

```clojure
{:address "Rua Homem Cristo Filho, n.º 62 – B, 3804-501 Aveiro",
 :archived-last-seen-at "2022-05-07T14:52:45Z", ;;<-----
 :name "OK Condutor",
 :href-id #uuid "abb64eb1-d043-3530-a3db-9edaecc18000",
 :distrito "Aveiro",
 :imt-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/HomemCristo.aspx",
 :concelho "Aveiro",
 :nec 1426,
 :cp7 "3804-501",
 :id #uuid "6dd53139-1797-3b3f-918a-57e7f2c695a5",
 :concelho-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Aveiro&Concelho=Aveiro"}
```

`:archived-last-seen-at` means that this school is "archived," and the last fetch was on May 7th, 2022.

### Snapshots

Each file in `snapshots` is a list of all schools online at that moment. We compare the new snapshot to the files in `parsed-data` to derive `:archived-last-seen-at`.

### When was the last fetch?

Check `last-fetch.txt` for the timestamp (epoch in milliseconds).

With each fetch, we [create a PR with the new data](https://github.com/codecadre/imt-school-addresses/pull/2). The description has a resume of what's changing.

## Unique IDs

Schools have an associated integer value that is unique for the most part. This is the school license number provided by IMT, sometimes called the "school number", "nec", or _alvará_. However, this number appears associated with [multiple schools with different numbers](https://github.com/codecadre/imt-school-addresses/blob/20b1d3a0a4d05c54a906b3c2f55d4ea92ac73d70/duplicates.txt), for multiple reasons. For instance, it might be that a school closed down and the same license was granted to a different school. Because of this, we added a UUID deterministically generated from

```UUID (school name + license nr. + address)```

## Overwrites

In some cases, information on the IMT school profile might not be accurate, for instance, the address might be an old one. Manually fixing these instances is outside the scope of the project. However, in cases where the address has a glaring mistake, we manually fix it with `overwrites.edn`. Here are a few examples:

- [Lago Azul](https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LagoAzul%e2%80%93FigueirodosVinhos588.aspx) zip code: `3620` should be `3260`.

## Duplicates


`duplicates.txt` is a list of *active* schools with the same license number, meaning, duplicates in the last snapshot.
## Background

This was originally done for [passaprimeira.xyz](https://www.passaprimeira.xyz).

[passaprimeira.xyz](https://www.passaprimeira.xyz) aims to bring transparency to the driving school business in Portugal by using publicly available data and presenting it in an accessible format. Originally, it was only a web app, but we decided to release some of the data and code so that others could benefit from it.

## Steps to reproduce this:

### Dependencies

To reproduce the parsing and data processing, you need [`babashka`](https://babashka.org/) and [`nbb`](https://github.com/babashka/nbb).

### Install

```
npm i
```

## Run

make sure temp folder exists.

```
bb run-all
```

Check `bb.edn` for a breakdown of the process.

## nrepl

```
bb nrepl
```

for `.clj` files and

```
nbb nrepl-server :port 1337
```

for `*.cljs` files


## License (code and open data)

The code is MIT licensed - basically, you can do what you want with the code, just give this project credit for it.

The data being reproduced here is assumed to be in the public domain. Additionally, when I stated the purpose in the [FOI I filled with IMT](https://www.flaviosousa.co/pedido-accesso-dados-publicos/), no objection was made on the grounds of it being made public.

According to the [Open Data Directive](https://digital-strategy.ec.europa.eu/en/policies/open-data), countries are encouraged to make public data accessible, regardless of the end use:

> clearly obliged member states to ‘encourage public sector bodies and public undertakings to produce and make available documents [...] in accordance with the principle of “open by design and by default’’.

[From Wikipedia](https://en.wikipedia.org/wiki/Directive_on_the_re-use_of_public_sector_information#Open_data_licensing)

Here, "encouraging" is the key word. Given that no explicit consent or license was given, in principle, there's always a chance that this project is using data beyond the scope of its intended use. We hope to show that initiatives like this bring about positive changes and that they further encourage government bodies to release data with explicit [Open Data licenses](https://en.wikipedia.org/wiki/Directive_on_the_re-use_of_public_sector_information#Open_data_licensing)

## Related projects

https://github.com/codecadre/imt-pass-rates

https://github.com/codecadre/melhordazona-web/
