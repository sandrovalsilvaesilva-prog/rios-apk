/*
  Ponte reservada para a etapa Android.
  No navegador comum, este arquivo nao faz nada.
  No APK futuro, ele devera receber PDFs compartilhados pelo Android
  e encaminhar para a rotina de importacao da Central.
*/

window.RetornoAndroidBridge = {
  receberPdfCompartilhado: async function arquivoRecebidoPlaceholder() {
    console.log("Android bridge aguardando implementacao nativa.");
  }
};
