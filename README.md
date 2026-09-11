# Alarme de Chegada

App separado do Super Despertador. Toca um alarme quando você chega perto de
um local marcado no mapa — útil pra não passar do ponto de ônibus/metrô
cochilando.

## Como funciona

1. Na tela principal, toca em **"+ Novo alarme de local"**
2. Escolhe o local tocando no mapa (mapa OpenStreetMap, gratuito, sem
   precisar de conta Google nem cartão de crédito)
3. Ajusta o raio (de 100m a 2000m) — quanto maior, mais cedo o alarme avisa
4. Dá um nome pro local (ex: "Minha parada") e salva

Quando você entrar nesse raio, o alarme toca em tela cheia, som alto — só
para segurando o botão por 2 segundos (evita desligar no reflexo, sem estar
de fato acordado).

## Como compilar

Mesmo processo do Super Despertador: sobe pra um repositório GitHub novo, o
Actions compila sozinho, baixa o APK do artefato.

## Permissões e avisos importantes

- **Localização em segundo plano**: essencial pro alarme funcionar com o app
  fechado (o cenário normal de uso — você não vai ficar com o app aberto
  olhando o mapa o tempo todo). O Android pede essa permissão numa tela
  separada das outras.
- **Para a Play Store**: essa permissão de localização em segundo plano é
  tratada como "sensível" pelo Google — a aprovação pode demorar mais, e
  costuma ser exigido um vídeo curto mostrando o app sendo usado de verdade,
  demonstrando por que a permissão é necessária.
- **Bateria/autostart**: mesmo esquema do Super Despertador — aceita as
  permissões pedidas na primeira abertura, e em aparelhos Xiaomi/Samsung/etc,
  libera manualmente nas configurações do fabricante se necessário.

## Limitações honestas

- O geofence (cerca virtual) do Android tem uma precisão de detecção de
  cerca de 50-150 metros, dependendo do GPS/sinal do aparelho — não espere
  precisão de metro a metro.
- Pode levar de alguns segundos a poucos minutos pra disparar depois de
  entrar na área, dependendo de como o sistema está monitorando a
  localização no momento (o Android otimiza isso pra economizar bateria).
