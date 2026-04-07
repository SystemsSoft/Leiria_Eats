package org.leria.eats.project.presentation

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRect
import platform.UIKit.*
import platform.WebKit.*
import platform.darwin.NSObject

private const val LEIRIA_LAT = 39.7436
private const val LEIRIA_LNG = -8.8071

private fun buildMapHtml(lat: Double, lng: Double) = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    *{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
    html,body{width:100%;height:100%;overflow:hidden;background:#061510;font-family:-apple-system,BlinkMacSystemFont,sans-serif}
    #map{position:absolute;top:0;left:0;right:0;bottom:0}
    #bar{
      position:absolute;bottom:0;left:0;right:0;z-index:2000;
      background:rgba(6,21,16,0.95);
      border-top:1px solid rgba(255,193,7,0.3);
      padding:14px 16px 40px;
      display:flex;align-items:center;gap:12px;
    }
    #confirmBtn{
      flex:1;height:48px;border:none;border-radius:14px;
      background:linear-gradient(90deg,#FFC107,#E65100);
      color:#061510;font-weight:700;font-size:15px;
      cursor:pointer;opacity:0.35;pointer-events:none;
      transition:opacity .25s;
    }
    #confirmBtn.active{opacity:1;pointer-events:all}
    #closeBtn{
      width:48px;height:48px;border:none;border-radius:14px;flex-shrink:0;
      background:rgba(255,255,255,0.08);
      border:1px solid rgba(255,255,255,0.12);
      color:#6EE7A0;font-size:20px;cursor:pointer;
    }
    #hint{
      position:absolute;top:14px;left:50%;transform:translateX(-50%);
      background:rgba(6,21,16,0.88);color:#F0FDF4;
      padding:8px 18px;border-radius:22px;font-size:13px;font-weight:500;
      border:1px solid rgba(255,193,7,0.4);
      pointer-events:none;white-space:nowrap;z-index:2000;
      transition:opacity .3s;
    }
    #hint.hidden{opacity:0;pointer-events:none}
    #addrPill{
      position:absolute;top:52px;left:16px;right:16px;z-index:2000;
      background:rgba(6,21,16,0.88);
      border:1px solid rgba(110,231,160,0.25);
      border-radius:14px;padding:10px 14px;
      display:none;
    }
    #addrPill.visible{display:block}
    #addrStreet{font-size:14px;font-weight:600;color:#F0FDF4;margin-bottom:2px}
    #addrCity{font-size:12px;color:rgba(240,253,244,0.55)}
    .leaflet-control-attribution{display:none!important}
    .leaflet-control-zoom{display:none!important}
    #zoomBtns{
      position:absolute;
      bottom:120px;
      right:12px;
      z-index:2000;
      display:flex;
      flex-direction:column;
      gap:6px;
    }
    #zoomIn,#zoomOut{
      width:42px;height:42px;border:none;border-radius:12px;
      background:rgba(6,21,16,0.92);
      border:1px solid rgba(255,193,7,0.25);
      color:#6EE7A0;font-size:22px;font-weight:700;
      cursor:pointer;display:flex;align-items:center;justify-content:center;
      box-shadow:0 2px 8px rgba(0,0,0,0.4);
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <div id="hint">Toque no mapa para selecionar</div>
  <div id="addrPill">
    <div id="addrStreet">A obter endereço…</div>
    <div id="addrCity"></div>
  </div>
  <div id="zoomBtns">
    <button id="zoomIn" onclick="map.zoomIn()">+</button>
    <button id="zoomOut" onclick="map.zoomOut()">−</button>
  </div>
  <div id="bar">
    <button id="closeBtn" onclick="closePicker()">✕</button>
    <button id="confirmBtn">📍 Confirmar Localização</button>
  </div>
  <script>
    var map=L.map('map',{zoomControl:false,attributionControl:false}).setView([$lat,$lng],14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,crossOrigin:true}).addTo(map);

    var marker=null, selectedLat=null, selectedLng=null;

    var goldIcon=L.divIcon({
      html:'<div style="width:26px;height:26px;background:linear-gradient(135deg,#FFC107,#E65100);border:3px solid #061510;border-radius:50% 50% 50% 0;transform:rotate(-45deg);box-shadow:0 3px 10px rgba(255,193,7,.6)"><div style="width:8px;height:8px;background:#061510;border-radius:50%;position:absolute;top:9px;left:9px"></div></div>',
      iconSize:[26,32],iconAnchor:[13,32],className:''
    });

    function reverseGeocode(lat,lng){
      document.getElementById('addrStreet').textContent='A obter endereço…';
      document.getElementById('addrCity').textContent='';
      document.getElementById('addrPill').classList.add('visible');
      fetch('https://nominatim.openstreetmap.org/reverse?lat='+lat+'&lon='+lng+'&format=json&accept-language=pt')
        .then(function(r){return r.json();})
        .then(function(d){
          var a=d.address||{};
          var road=a.road||a.pedestrian||a.footway||'Rua sem nome';
          var num=a.house_number?', '+a.house_number:'';
          var city=a.city||a.town||a.village||a.municipality||'';
          var cp=a.postcode||'';
          document.getElementById('addrStreet').textContent=road+num;
          document.getElementById('addrCity').textContent=[cp,city].filter(Boolean).join('  ');
        })
        .catch(function(){
          document.getElementById('addrStreet').textContent=lat.toFixed(5)+', '+lng.toFixed(5);
        });
    }

    map.on('click',function(e){
      selectedLat=e.latlng.lat; selectedLng=e.latlng.lng;
      if(marker)map.removeLayer(marker);
      marker=L.marker([selectedLat,selectedLng],{icon:goldIcon}).addTo(map);
      document.getElementById('hint').classList.add('hidden');
      document.getElementById('confirmBtn').classList.add('active');
      reverseGeocode(selectedLat,selectedLng);
    });

    function closePicker(){
      window.webkit.messageHandlers.mapAction.postMessage('close');
    }
    document.getElementById('confirmBtn').addEventListener('click',function(){
      if(selectedLat!==null){
        window.webkit.messageHandlers.mapAction.postMessage('select:'+selectedLat+','+selectedLng);
      }
    });
  </script>
</body>
</html>
""".trimIndent()

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
) {
    // Stable callbacks captured once
    val onDismissRef = rememberUpdatedState(onDismiss)
    val onLocationRef = rememberUpdatedState(onLocationSelected)

    DisposableEffect(Unit) {
        val html = buildMapHtml(LEIRIA_LAT, LEIRIA_LNG)

        // Build WKWebView with message handler
        val config = WKWebViewConfiguration()
        val controller = WKUserContentController()

        val handler = object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage
            ) {
                val msg = didReceiveScriptMessage.body as? String ?: return
                when {
                    msg == "close" -> {
                        onDismissRef.value()
                    }
                    msg.startsWith("select:") -> {
                        val coords = msg.removePrefix("select:").split(",")
                        val lat = coords.getOrNull(0)?.toDoubleOrNull()
                        val lng = coords.getOrNull(1)?.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            onLocationRef.value(lat, lng)
                        }
                    }
                }
            }
        }
        controller.addScriptMessageHandler(handler, name = "mapAction")
        config.userContentController = controller

        // Create UIViewController hosting the WKWebView
        val vc = UIViewController()
        vc.modalPresentationStyle = UIModalPresentationFullScreen

        val webView = WKWebView(frame = cValue<CGRect>(), configuration = config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.scrollView.bounces = false
        webView.backgroundColor = UIColor.blackColor
        webView.opaque = false

        vc.view.addSubview(webView)
        vc.view.backgroundColor = UIColor.blackColor

        // Full-screen constraints
        NSLayoutConstraint.activateConstraints(listOf(
            webView.topAnchor.constraintEqualToAnchor(vc.view.topAnchor),
            webView.bottomAnchor.constraintEqualToAnchor(vc.view.bottomAnchor),
            webView.leadingAnchor.constraintEqualToAnchor(vc.view.leadingAnchor),
            webView.trailingAnchor.constraintEqualToAnchor(vc.view.trailingAnchor)
        ))

        webView.loadHTMLString(html, baseURL = null)

        // Present modally
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
        val presenter = rootVc?.presentedViewController ?: rootVc
        presenter?.presentViewController(vc, animated = true, completion = null)

        onDispose {
            vc.dismissViewControllerAnimated(true, completion = null)
        }
    }
}