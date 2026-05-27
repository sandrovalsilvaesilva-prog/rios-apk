(function(){
  function base64ParaArquivo(item){
    var binario = atob(item.data || "");
    var bytes = new Uint8Array(binario.length);
    for(var i=0;i<binario.length;i++) bytes[i] = binario.charCodeAt(i);
    return new File([bytes], item.name || "arquivo", { type: item.mime || "application/octet-stream" });
  }

  window.receberArquivosAndroid = function(tipo, itens){
    try{
      var arquivos = (itens || []).map(base64ParaArquivo);
      if(!arquivos.length) return;
      if(tipo === "pdf" && typeof carregarPDFs === "function") carregarPDFs({ target:{ files: arquivos } });
      if(tipo === "foto" && typeof addFotos === "function") addFotos(arquivos);
    }catch(e){
      alert("Nao foi possivel receber o arquivo selecionado no Android.");
      console.error(e);
    }
  };

  function prepararBotoesAndroid(){
    if(!window.RiosAndroid) return;
    var pdfBtn = document.querySelector("button[onclick*='pdfInput']");
    var galeriaBtn = document.querySelector("button[onclick*='galleryInput']");
    var cameraBtn = document.querySelector("button[onclick*='abrirCamera'],button[onclick*='cameraInput']");

    if(pdfBtn) pdfBtn.onclick = function(){ window.RiosAndroid.openPdfPicker(); };
    if(galeriaBtn) galeriaBtn.onclick = function(){ window.RiosAndroid.openGalleryPicker(); };
    if(cameraBtn) cameraBtn.onclick = function(){ window.RiosAndroid.openCamera(); };
  }

  document.addEventListener("DOMContentLoaded", prepararBotoesAndroid);
})();
