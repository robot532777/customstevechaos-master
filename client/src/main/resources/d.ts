declare class PlayerExtensions {
    static jump(): void;

    static setMotionX(motionX: number): void;

    static setMotionY(motionY: number): void;

    static setMotionZ(motionZ: number): void;

    static setMotions(x: number, y: number, z: number): void;

    static setSneaking(sneaking: boolean): void;
}

declare class PluginMessages {
    static on(handle: any, channel: string, listener: (e: ByteBuf) => void, priority?: number): void;

    static off(channel: string, listener: (e: ByteBuf) => void): void;

    static off(handle: any): void;

    static emit(channel: string, data: ByteBuf): void;
}

declare class WorldExtensions {
    static setTotalTime(time: number): void;

    static setTime(time: number): void;
}

// BOTH
declare class Display {
    static getTitle(): string;

    static isFullscreen(): boolean;

    static setTitle(title: string): void;

    static isActive(): boolean;

    static getAdapter(): string;

    static getVersion(): string;

    static getX(): number;

    static getY(): number;

    static getWidth(): number;

    static getHeight(): number;
}

declare class Draw {
    static getFps(): number;

    static drawHorizontalLine(startX: number, endX: number, y: number, color: number): void;

    static drawVerticalLine(x: number, startY: number, endY: number, color: number): void;

    static drawRect(left: number, top: number, right: number, bottom: number, color: number): void;

    static drawStringWithShadow(text: string, x: number, y: number, color?: number): number;

    static drawString(text: string, x: number, y: number, color?: number, dropShadow?: boolean): number;

    static getStringWidth(text: string): number;

    static getCharWidth(char: number): number;

    static trimStringToWidth(text: string, width: number, reverse?: boolean): string;

    static drawSplitString(text: string, x: number, y: number, wrapWidth: number, textColor?: number): void;

    static getResolution(): ScaledResolution;
}

declare type EnumFacing = 'DOWN' | 'UP' | 'NORTH' | 'SOUTH' | 'WEST' | 'EAST';
declare type EquipmentSlot = 'FEET' | 'LEGS' | 'CHEST' | 'HEAD' | 'OFFHAND';

declare type ChatEvent = {
    message: string,
    readonly command: boolean,
    cancelled: boolean,
};
declare type KeyPressEvent = {
    key: number,
    cancelled: boolean,
};

declare type EventAction =
    'chat_send' |
    'gui_overlay_render' |
    'game_loop' |
    'static KEY_press' |
    'server_connect' |
    'server_switch' |
    'overlay_render' |
    'game_tick_pre' |
    'game_tick_post' |
    string;

declare type Listener<T> = (event: T) => void;

declare class Events {
    static on<T>(listener: any, action: EventAction, consumer: Listener<T>, priority?: number): void;

    static off<T>(action: EventAction, consumer: Listener<T>): void;

    static off(listener: any): void;

    static post<T>(action: EventAction | string, event: T): T;
}

declare type GameType = 'NOT_SET' | 'SURVIVAL' | 'CREATIVE' | 'ADVENTURE' | 'SPECTATOR';

declare class Inventory {
    static getMaxDamage(slot: number): number | 0;

    static getItemDamage(slot: number): number | 0;

    static getDurability(slot: number): number | 0;

    static getItemDisplayName(slot: number): string | '';

    static getCount(slot: number): number | 0;

    static isEquipped(slot: number): boolean | false;

    static getActiveSlot(): number | 0;
}

declare class Keyboard {
    static KEY_NONE: number;
    static KEY_ESCAPE: number;
    static KEY_1: number;
    static KEY_2: number;
    static KEY_3: number;
    static KEY_4: number;
    static KEY_5: number;
    static KEY_6: number;
    static KEY_7: number;
    static KEY_8: number;
    static KEY_9: number;
    static KEY_0: number;
    static KEY_MINUS: number; /* - on main keyboard */
    static KEY_EQUALS: number;
    static KEY_BACK: number; /* backspace */
    static KEY_TAB: number;
    static KEY_Q: number;
    static KEY_W: number;
    static KEY_E: number;
    static KEY_R: number;
    static KEY_T: number;
    static KEY_Y: number;
    static KEY_U: number;
    static KEY_I: number;
    static KEY_O: number;
    static KEY_P: number;
    static KEY_LBRACKET: number;
    static KEY_RBRACKET: number;
    static KEY_RETURN: number; /* Enter on main keyboard */
    static KEY_LCONTROL: number;
    static KEY_A: number;
    static KEY_S: number;
    static KEY_D: number;
    static KEY_F: number;
    static KEY_G: number;
    static KEY_H: number;
    static KEY_J: number;
    static KEY_K: number;
    static KEY_L: number;
    static KEY_SEMICOLON: number;
    static KEY_APOSTROPHE: number;
    static KEY_GRAVE: number; /* accent grave */
    static KEY_LSHIFT: number;
    static KEY_BACKSLASH: number;
    static KEY_Z: number;
    static KEY_X: number;
    static KEY_C: number;
    static KEY_V: number;
    static KEY_B: number;
    static KEY_N: number;
    static KEY_M: number;
    static KEY_COMMA: number;
    static KEY_PERIOD: number; /* . on main keyboard */
    static KEY_SLASH: number; /* / on main keyboard */
    static KEY_RSHIFT: number;
    static KEY_MULTIPLY: number; /* * on numeric keypad */
    static KEY_LMENU: number; /* left Alt */
    static KEY_SPACE: number;
    static KEY_CAPITAL: number;
    static KEY_F1: number;
    static KEY_F2: number;
    static KEY_F3: number;
    static KEY_F4: number;
    static KEY_F5: number;
    static KEY_F6: number;
    static KEY_F7: number;
    static KEY_F8: number;
    static KEY_F9: number;
    static KEY_F10: number;
    static KEY_NUMLOCK: number;
    static KEY_SCROLL: number; /* Scroll Lock */
    static KEY_NUMPAD7: number;
    static KEY_NUMPAD8: number;
    static KEY_NUMPAD9: number;
    static KEY_SUBTRACT: number; /* - on numeric keypad */
    static KEY_NUMPAD4: number;
    static KEY_NUMPAD5: number;
    static KEY_NUMPAD6: number;
    static KEY_ADD: number; /* + on numeric keypad */
    static KEY_NUMPAD1: number;
    static KEY_NUMPAD2: number;
    static KEY_NUMPAD3: number;
    static KEY_NUMPAD0: number;
    static KEY_DECIMAL: number; /* . on numeric keypad */
    static KEY_F11: number;
    static KEY_F12: number;
    static KEY_F13: number; /*                     (NEC PC98) */
    static KEY_F14: number; /*                     (NEC PC98) */
    static KEY_F15: number; /*                     (NEC PC98) */
    static KEY_F16: number; /* Extended Function keys - (Mac) */
    static KEY_F17: number;
    static KEY_F18: number;
    static KEY_KANA: number; /* (Japanese keyboard)            */
    static KEY_F19: number; /* Extended Function keys - (Mac) */
    static KEY_CONVERT: number; /* (Japanese keyboard)            */
    static KEY_NOCONVERT: number; /* (Japanese keyboard)            */
    static KEY_YEN: number; /* (Japanese keyboard)            */
    static KEY_NUMPADEQUALS: number; /*=on numeric keypad (NEC PC98) */
    static KEY_CIRCUMFLEX: number; /* (Japanese keyboard)            */
    static KEY_AT: number; /*                     (NEC PC98) */
    static KEY_COLON: number; /*                     (NEC PC98) */
    static KEY_UNDERLINE: number; /*                     (NEC PC98) */
    static KEY_KANJI: number; /* (Japanese keyboard)            */
    static KEY_STOP: number; /*                     (NEC PC98) */
    static KEY_AX: number; /*                     (Japan AX) */
    static KEY_UNLABELED: number; /*                        (J3100) */
    static KEY_NUMPADENTER: number; /* Enter on numeric keypad */
    static KEY_RCONTROL: number;
    static KEY_SECTION: number; /* Section symbol (Mac) */
    static KEY_NUMPADCOMMA: number; /* , on numeric keypad (NEC PC98) */
    static KEY_DIVIDE: number; /* / on numeric keypad */
    static KEY_SYSRQ: number;
    static KEY_RMENU: number; /* right Alt */
    static KEY_FUNCTION: number; /* Function (Mac) */
    static KEY_PAUSE: number; /* Pause */
    static KEY_HOME; /* Home on arrow keypad */
    static KEY_UP: number; /* UpArrow on arrow keypad */
    static KEY_PRIOR: number; /* PgUp on arrow keypad */
    static KEY_LEFT: number; /* LeftArrow on arrow keypad */
    static KEY_RIGHT: number; /* RightArrow on arrow keypad */
    static KEY_END: number; /* End on arrow keypad */
    static KEY_DOWN: number; /* DownArrow on arrow keypad */
    static KEY_NEXT: number; /* PgDn on arrow keypad */
    static KEY_INSERT: number; /* Insert on arrow keypad */
    static KEY_DELETE: number; /* Delete on arrow keypad */
    static KEY_CLEAR: number; /* Clear key (Mac) */
    static KEY_LMETA: number; /* Left Windows/Option key */
    static KEY_RMETA: number; /* Right Windows/Option key */
    static KEY_APPS: number; /* AppMenu key */
    static KEY_POWER: number;
    static KEY_SLEEP: number;

    static isKeyDown(key: number): boolean;

    static getEventKey(): number;

    static getEventCharacter(): number;

    static getEventKeyState(): boolean;

    static getKeyName(key: number): string;

    static getKeyIndex(keyName: string): number;
}

declare class ScaledResolution {
    getScaledWidth(): number;

    getScaledHeight(): number;

    getScaledWidth_double(): number;

    getScaledHeight_double(): number;

    getScaleFactor(): number;
}

declare class Mouse {
    static isButtonDown(button: number): boolean;

    static getButtonName(button: number): string | undefined;

    static getButtonIndex(buttonName: string): number | -1;

    static getEventButton(): number;

    static getEventButtonState(): boolean;

    static getEventDX(): number;

    static getEventDY(): number;

    static getEventX(): number;

    static getEventY(): number;

    static getEventDWheel(): number;

    static getX(): number;

    static getY(): number;

    static getDX(): number;

    static getDY(): number;

    static getDWheel(): number;

    static getButtonCount(): number;

    static hasWheel(): boolean;

    static isGrabbed(): boolean;

    static getMouseX(resolution: ScaledResolution): number;

    static getMouseY(resolution: ScaledResolution): number;
}

declare class Player {
    static getPosX(): number | 0.0;

    static getPosY(): number | 0.0;

    static getPosZ(): number | 0.0;

    static getYaw(): number | 0.0;

    static getPitch(): number | 0.0;

    static getHorizontalFacing(): EnumFacing | undefined;

    static getVerticalFacing(): EnumFacing | undefined;

    static getHealth(): number | 0.0;

    static getMaxHealth(): number | 0.0;

    static isOnGround(): boolean | false;

    // TODO getActivePotionEffects

    static getGameType(): GameType | undefined;

    static getName(): string;

    static isConnected(): boolean;

    // TODO etTargetBlockPos(reach: number): BlockPos | undefined;
}

declare class Runtime {
    static exit(code: number): void;

    static halt(code: number): void;

    static freeMemory(): number;

    static maxMemory(): number;

    static totalMemory(): number;
}

declare class System {
    static exit(code: number): void;

    static halt(code: number): void;

    static getOsName(): string;

    static getOsVersion(): string;

    static currentTimeMillis(): number;

    static nanoTime(): number;
}

declare class World {
    static getTime(): number | 0;

    static getTotalTime(): number | 0;
}

declare class ByteBuf {
    readBoolean(): boolean;
    readInt(): number;
    readChar(): number;
}
