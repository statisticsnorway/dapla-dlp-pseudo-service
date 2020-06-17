#!/bin/bash

MAX=500000
echo "["
i=1
while [ $i -le $MAX ]; do
  echo -ne "{\"id\":$i, \"navn\": \"Donald Duck $((i))\", \"alder\": 42, \"adresse\": {\"adresselinjer\": [\"Andeveien $((i))\", \"Andedammen\"], \"postnummer\": \"3158\", \"poststed\": \"Andebu\"}}"
  if [ "$i" -ne "$MAX" ]; then
    echo ","
  fi
  let i=i+1
done
echo "]"