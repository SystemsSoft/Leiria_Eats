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
<!DOCTYPE html><html>
<head>
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    *{margin:0;padding:0;box-sizing:border-box}
    html,body,#map{width:100%;height:100%;background:#061510}
    #bar{
      position:absolute;bottom:0;left:0;right:0;
      background:rgba(6,21,16,0.93);
      border-top:1px solid rgba(255,193,7,0.3);
      padding:14px 16px;
      display:flex;align-items:center;gap:12px;
      z-index:1000;
    }
    #confirmBtn{
      flex:1;height:46px;border:none;border-radius:12px;
      background:linear-gradient(90deg,#FFC107,#E65100);
      color:#061510;font-weight:700;font-size:15px;
      cursor:pointer;opacity:0.4;pointer-events:none;
    }
    #confirmBtn.active{opacity:1;pointer-events:all;}
    #closeBtn{
      width:46px;height:46px;border:none;border-radius:12px;
      background:rgba(255,255,255,0.08);
      color:#6EE7A0;font-size:20px;cursor:pointer;
    }
    #hint{
      position:absolute;top:12px;left:50%;transform:translateX(-50%);
      background:rgba(6,21,16,0.85);color:#F0FDF4;
      padding:8px 16px;border-radius:20px;font-size:13px;
      border:1px solid rgba(255,193,7,0.4);
      pointer-events:none;white-space:nowrap;z-index:1000;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <div id="hint">Toque no mapa para selecionar</div>
  <div id="bar">
    <button id="closeBtn" onclick="closePicker()">✕</button>
    <button id="confirmBtn" id="confirmBtn">Confirmar Localização</button>
  </div>
  <script>
    var map=L.map('map',{zoomControl:true}).setView([$lat,$lng],14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(map);
    var marker=null;
    var selectedLat=null, selectedLng=null;
    var goldIcon=L.divIcon({
      html:'<div style="width:22px;height:22px;background:#FFC107;border:3px solid #061510;border-radius:50%;box-shadow:0 2px 8px rgba(0,0,0,.6)"></div>',
      iconSize:[22,22],iconAnchor:[11,11],className:''
    });
    map.on('click',function(e){
      selectedLat=e.latlng.lat; selectedLng=e.latlng.lng;
      if(marker)map.removeLayer(marker);
      marker=L.marker([selectedLat,selectedLng],{icon:goldIcon}).addTo(map);
      document.getElementById('hint').style.display='none';
      document.getElementById('confirmBtn').classList.add('active');
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
</body></html>
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