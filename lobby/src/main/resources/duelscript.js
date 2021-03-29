(function (self) {
    let enable = false
    let text = []

    function readString(buf) {
        const length = buf.readInt()
        let result = ""
        for (let i = 0; i < length; i++) {
            result += String.fromCharCode(buf.readInt())
        }
        return result
    }

    PluginMessages.on(self, "stepbystep:duelinfo", function (buf) {
        enable = buf.readBoolean()
        text = []
        if (enable) {
            const textLength = buf.readInt()
            for (let i = 0; i < textLength; i++) {
                text.push(readString(buf))
            }
        }
    })

    Events.on(self, "gui_overlay_render", function (e) {
        if (!enable || text.length === 0) return

        const textHeight = 15
        const factor = Draw.getResolution().getScaleFactor()
        let currentHeight = (Display.getHeight() - text.length * textHeight) / (factor * 2)

        for (let i = 0; i < text.length; i++) {
            Draw.drawString(text[i], 10, currentHeight)
            currentHeight += textHeight
        }
    })
})(this)
