Colocar aqui as bibliotecas locais para funcionamento 100% offline:

- pdf.min.js
- pdf.worker.min.js
- pdf-lib.min.js

Depois de incluir estes arquivos, ajustar as tres referencias no index.html:

https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js
para
vendor/pdf.min.js

https://cdn.jsdelivr.net/npm/pdf-lib/dist/pdf-lib.min.js
para
vendor/pdf-lib.min.js

https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js
para
vendor/pdf.worker.min.js
