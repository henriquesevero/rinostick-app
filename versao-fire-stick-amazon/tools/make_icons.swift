// Gera o ícone e o banner do RinoStick (PNG) com CoreGraphics.
// Uso (na pasta da versão):  swift tools/make_icons.swift [prévia-1024.png]
// Saída: res/drawable-xxxhdpi/rinostick_icon.png (192 px) e res/drawable-xhdpi/rinostick_banner.png (640x360 px)
// (nomes trocados na v1.4: a tela da Amazon guardava em cache a imagem antiga pelo nome)
//
// Estilo: só o nome ("Rino" + "Stick" em cores diferentes), dentro de uma cápsula de
// vidro no estilo Liquid Glass (Apple), sobre um fundo em degradê. Cores: veja `themes`.

import AppKit
import CoreText

func rgb(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat, _ a: CGFloat = 1) -> CGColor { CGColor(srgbRed: r, green: g, blue: b, alpha: a) }

/// Paleta. Escolha com a variável THEME (padrão: apple).
///   THEME=goldengate swift tools/make_icons.swift
struct Theme {
    let top, bottom: CGColor          // degradê do fundo
    let glowTop, glowBottom: CGColor  // brilhos difusos (canto de cima à esquerda / de baixo à direita)
    let rino, stick: CGColor          // cores do nome
    var glassBody: [CGColor] = [rgb(1, 1, 1, 0.26), rgb(1, 1, 1, 0.08), rgb(1, 1, 1, 0.14)]
    var glassRim: [CGColor] = [rgb(1, 1, 1, 0.90), rgb(1, 1, 1, 0.30), rgb(1, 1, 1, 0.16), rgb(1, 1, 1, 0.50)]
    var glassShadow: CGColor = rgb(0, 0.05, 0.16, 0.40)
    var textShadow: CGColor = rgb(0, 0.05, 0.2, 0.25)
}

let themes: [String: Theme] = [
    // Azul royal + laranja (inspirado no azul e laranja dos Knicks)
    "knicks": Theme(top: rgb(0.000, 0.459, 0.773), bottom: rgb(0.000, 0.290, 0.565),
                    glowTop: rgb(0.35, 0.78, 1.0, 0.45), glowBottom: rgb(0.0, 0.16, 0.38, 0.55),
                    rino: rgb(1, 1, 1), stick: rgb(0.961, 0.518, 0.149)),
    // Golden Gate: céu de entardecer (azul-marinho → laranja internacional), neblina e dourado
    "goldengate": Theme(top: rgb(0.114, 0.173, 0.302), bottom: rgb(0.753, 0.212, 0.173),
                        glowTop: rgb(0.62, 0.68, 0.82, 0.30), glowBottom: rgb(1.0, 0.72, 0.32, 0.55),
                        rino: rgb(0.965, 0.937, 0.894), stick: rgb(1.0, 0.420, 0.200)),
    // Apple (claro, monocromático): alumínio claro, vidro fosco branco, texto quase preto e cinza
    "apple": Theme(top: rgb(0.984, 0.984, 0.992), bottom: rgb(0.882, 0.886, 0.906),
                   glowTop: rgb(1, 1, 1, 0.95), glowBottom: rgb(0.74, 0.75, 0.79, 0.45),
                   rino: rgb(0.114, 0.114, 0.122), stick: rgb(0.525, 0.525, 0.545),
                   glassBody: [rgb(1, 1, 1, 0.85), rgb(1, 1, 1, 0.55), rgb(1, 1, 1, 0.70)],
                   glassRim: [rgb(1, 1, 1, 1.0), rgb(1, 1, 1, 0.6), rgb(0, 0, 0, 0.05), rgb(0, 0, 0, 0.10)],
                   glassShadow: rgb(0.1, 0.1, 0.15, 0.18),
                   textShadow: rgb(0, 0, 0, 0)),
]
let theme = themes[ProcessInfo.processInfo.environment["THEME"] ?? "apple"] ?? themes["apple"]!
let white = theme.rino
let orange = theme.stick
let space      = CGColorSpace(name: CGColorSpace.sRGB)!

func gradient(_ colors: [CGColor], _ locs: [CGFloat]? = nil) -> CGGradient {
    CGGradient(colorsSpace: space, colors: colors as CFArray, locations: locs)!
}

/// Fundo em degradê com dois brilhos difusos (cores do tema).
func background(_ c: CGContext, _ w: CGFloat, _ h: CGFloat, radius: CGFloat) {
    c.addPath(CGPath(roundedRect: CGRect(x: 0, y: 0, width: w, height: h), cornerWidth: radius, cornerHeight: radius, transform: nil))
    c.clip()
    c.drawLinearGradient(gradient([theme.top, theme.bottom]), start: .zero, end: CGPoint(x: 0, y: h), options: [])
    let m = max(w, h)
    c.drawRadialGradient(gradient([theme.glowTop, theme.glowTop.copy(alpha: 0)!]),
                         startCenter: CGPoint(x: w * 0.18, y: h * 0.12), startRadius: 0,
                         endCenter: CGPoint(x: w * 0.18, y: h * 0.12), endRadius: m * 0.55, options: [])
    c.drawRadialGradient(gradient([theme.glowBottom, theme.glowBottom.copy(alpha: 0)!]),
                         startCenter: CGPoint(x: w * 0.90, y: h * 1.0), startRadius: 0,
                         endCenter: CGPoint(x: w * 0.90, y: h * 1.0), endRadius: m * 0.60, options: [])
}

/// Cápsula de vidro (Liquid Glass): corpo translúcido, reflexo no topo, borda
/// brilhante (mais forte em cima e embaixo) e sombra suave.
func glass(_ c: CGContext, _ r: CGRect, radius: CGFloat) {
    let path = CGPath(roundedRect: r, cornerWidth: radius, cornerHeight: radius, transform: nil)
    let u = min(r.width, r.height)

    // sombra
    c.saveGState()
    c.setShadow(offset: CGSize(width: 0, height: -u * 0.06), blur: u * 0.22, color: theme.glassShadow)
    c.addPath(path); c.setFillColor(rgb(1, 1, 1, 0.08)); c.fillPath()
    c.restoreGState()

    // corpo translúcido + reflexo no topo
    c.saveGState()
    c.addPath(path); c.clip()
    c.drawLinearGradient(gradient(theme.glassBody, [0, 0.55, 1]),
                         start: CGPoint(x: 0, y: r.minY), end: CGPoint(x: 0, y: r.maxY), options: [])
    c.drawRadialGradient(gradient([rgb(1, 1, 1, 0.30), rgb(1, 1, 1, 0)]),
                         startCenter: CGPoint(x: r.midX, y: r.minY - r.height * 0.35), startRadius: 0,
                         endCenter: CGPoint(x: r.midX, y: r.minY - r.height * 0.35), endRadius: r.height * 0.95, options: [])
    c.restoreGState()

    // borda com brilho em degradê
    c.saveGState()
    c.addPath(CGPath(roundedRect: r.insetBy(dx: u * 0.008, dy: u * 0.008), cornerWidth: radius, cornerHeight: radius, transform: nil))
    c.setLineWidth(max(1.2, u * 0.022))
    c.replacePathWithStrokedPath(); c.clip()
    c.drawLinearGradient(gradient(theme.glassRim, [0, 0.25, 0.70, 1]),
                         start: CGPoint(x: r.midX, y: r.minY), end: CGPoint(x: r.midX, y: r.maxY),
                         options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
    c.restoreGState()
}

for f in ["Inter-SemiBold", "Inter-Bold"] {
    CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: "assets/fonts/\(f).ttf") as CFURL, .process, nil)
}

func line(_ parts: [(String, CGColor)], size: CGFloat, font name: String = "Inter-SemiBold") -> CTLine {
    let font = CTFontCreateWithName(name as CFString, size, nil)
    let s = NSMutableAttributedString()
    for (t, col) in parts {
        s.append(NSAttributedString(string: t, attributes: [
            NSAttributedString.Key(kCTFontAttributeName as String): font,
            NSAttributedString.Key(kCTForegroundColorAttributeName as String): col,
            NSAttributedString.Key(kCTKernAttributeName as String): -size * 0.02]))
    }
    return CTLineCreateWithAttributedString(s)
}

func width(_ l: CTLine) -> CGFloat { CGFloat(CTLineGetTypographicBounds(l, nil, nil, nil)) }

/// Desenha a linha centralizada em x, com a linha de base em `baseline` (y para baixo).
func draw(_ c: CGContext, _ l: CTLine, centerX: CGFloat, baseline: CGFloat) {
    c.saveGState()
    c.textMatrix = CGAffineTransform(scaleX: 1, y: -1)
    c.setShadow(offset: CGSize(width: 0, height: -1), blur: 3, color: theme.textShadow)
    c.textPosition = CGPoint(x: centerX - width(l) / 2, y: baseline)
    CTLineDraw(l, c)
    c.restoreGState()
}

func render(_ w: Int, _ h: Int, _ body: (CGContext) -> Void) -> Data {
    let c = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                      bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    c.translateBy(x: 0, y: CGFloat(h)); c.scaleBy(x: 1, y: -1)   // y para baixo
    body(c)
    return NSBitmapImageRep(cgImage: c.makeImage()!).representation(using: .png, properties: [:])!
}

func save(_ d: Data, _ path: String) {
    try! FileManager.default.createDirectory(atPath: (path as NSString).deletingLastPathComponent, withIntermediateDirectories: true)
    try! d.write(to: URL(fileURLWithPath: path))
    print("ok: \(path)")
}

/// Ícone quadrado: "Rino" / "Stick" em duas linhas dentro do vidro.
func icon(_ c: CGContext, _ s: CGFloat) {
    background(c, s, s, radius: s * 0.225)
    let g = CGRect(x: s * 0.12, y: s * 0.20, width: s * 0.76, height: s * 0.60)
    glass(c, g, radius: s * 0.16)
    let size = s * 0.205
    draw(c, line([("Rino", white)], size: size, font: "Inter-Bold"), centerX: s / 2, baseline: s * 0.475)
    draw(c, line([("Stick", orange)], size: size, font: "Inter-Bold"), centerX: s / 2, baseline: s * 0.69)
}

/// Fire TV: a tela da Amazon desenha o ícone de apps instalados fora da loja num
/// quadrado no meio de um quadro cinza (#5A5A5A). Este ícone usa o mesmo cinza de
/// fundo (sem cantos) para não aparecer divisão, com a cápsula de vidro no meio.
let amazonTile = rgb(0x5A / 255.0, 0x5A / 255.0, 0x5A / 255.0)   // #5A5A5A (medido nos pixels da tela da Amazon)
func iconFire(_ c: CGContext, _ s: CGFloat) {
    c.setFillColor(amazonTile); c.fill(CGRect(x: 0, y: 0, width: s, height: s))
    let l = line([("Rino", theme.rino), ("Stick", theme.stick)], size: s * 0.155, font: "Inter-Bold")
    let tw = width(l)
    let ph = s * 0.30, padX = s * 0.06
    let pill = CGRect(x: (s - tw) / 2 - padX, y: (s - ph) / 2, width: tw + padX * 2, height: ph)
    glass(c, pill, radius: ph / 2)
    draw(c, l, centerX: s / 2, baseline: s / 2 + s * 0.056)
}

/// Simulação do quadro da tela da Amazon (para conferir antes de instalar).
func amazonTileMock(_ c: CGContext, _ w: CGFloat, _ h: CGFloat, _ iconDraw: (CGContext, CGFloat) -> Void) {
    c.setFillColor(rgb(0.035, 0.035, 0.035)); c.fill(CGRect(x: 0, y: 0, width: w, height: h))
    let tile = CGRect(x: w * 0.05, y: h * 0.08, width: w * 0.9, height: h * 0.84)
    c.saveGState()
    c.addPath(CGPath(roundedRect: tile, cornerWidth: h * 0.1, cornerHeight: h * 0.1, transform: nil)); c.clip()
    c.setFillColor(amazonTile); c.fill(tile)
    c.translateBy(x: tile.midX - tile.height / 2, y: tile.minY)
    iconDraw(c, tile.height)
    c.restoreGState()
}

if let mock = ProcessInfo.processInfo.environment["MOCK"] {
    save(render(680, 360) { c in amazonTileMock(c, 680, 360) { icon($0, $1) } }, mock + "-A.png")
    save(render(680, 360) { c in amazonTileMock(c, 680, 360) { iconFire($0, $1) } }, mock + "-B.png")
    exit(0)
}

// prévia grande (opcional)
if CommandLine.arguments.count > 1 {
    save(render(1024, 1024) { icon($0, 1024) }, CommandLine.arguments[1])
}

// ícone 192x192. Na versão Fire TV usa o ícone com fundo cinza da Amazon (iconFire),
// porque a tela da Amazon mostra o ícone no meio de um quadro cinza; no Google TV,
// o ícone claro (a tela inicial do Google usa o banner).
let isFire = FileManager.default.currentDirectoryPath.contains("versao-fire")
save(render(192, 192) { isFire ? iconFire($0, 192) : icon($0, 192) }, "res/drawable-xxxhdpi/rinostick_icon.png")

// banner 640x360: o nome numa linha, dentro da cápsula de vidro
save(render(640, 360) { c in
    let w: CGFloat = 640, h: CGFloat = 360
    background(c, w, h, radius: 0)
    let l = line([("Rino", white), ("Stick", orange)], size: 80)
    let tw = width(l)
    let ph: CGFloat = 138, padX: CGFloat = 46
    let pill = CGRect(x: (w - tw) / 2 - padX, y: (h - ph) / 2, width: tw + padX * 2, height: ph)
    glass(c, pill, radius: ph / 2)
    draw(c, l, centerX: w / 2, baseline: h / 2 + 28)
}, "res/drawable-xhdpi/rinostick_banner.png")
