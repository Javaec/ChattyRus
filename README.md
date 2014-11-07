ChattyRus
=========

Chatty for Twitch Russian version

Chatty для Twitch Русская версия

Добавлено в русской версии:
- перевод
- обработка подписок:
- - выгрузка последнего подписчика
- - выгрузка количества подписчиков
- - доступные форматы: .txt .png (автоматический рендер картинки, шрифт в font2.png, можно кастомизировать)

Лицензия
=========

ChattyRus наследует MIT лицензию от Chatty.

Это озночает, что ChattyRus может быть использован и модифицирован кем угодно без запроса дополнительных разрешений.
В случае распространения ChattyRus, копии всех использованных лицензий (в том числе и компонентов), должны быть приложены.


Установка
=========

Установите java: https://java.com/ru/download/

Скачайте ChattyRus: https://github.com/Javaec/ChattyRus/archive/master.zip

Распакуйте

В папке \release вы найдете всё, что необходимо:

- config.txt
- font2.png
- run_with_console.bat


Запуск
=========

- Запускайте \release\Chatty.jar (открыть с помощью Java(TM))
- - Опционально: вы можете запустить ChattyRus с помощью run_with_console.bat (запуск с консолью)
- Если появится окно с новостями - можете закрыть его
- "Создать аккаунт..."
- "Запросить данные аккаунта"
- "Открыть (браузер по-умолчанию)"
- Открылась страница браузера, авторизация пройдена, можете закрыть её
- В Chatty жмите "Готово"

- "Канал:" - название интересующего вас канала
- "Подключение"


config.txt
=========

offset:0; //смещение (не трогайте)

time:1440; //время в минутах, за которой выбираются сабы

todaytext:"Sub today:"; //надпись для сабов сегодня

lasttext:"Last sub:"; //надпись для последнего саба


font2.png
=========
Файл с глифами для шрифта. Вы можете отредактировать его для установки своего шрифта.



Chatty Original
=========

Chatty is a Twitch Chat Client for Desktop written in Java featuring many
Twitch specific features.

Website: http://getchatty.sourceforge.net
E-Mail: chattyclient@gmail.com
Twitter: @ChattyClient (https://twitter.com/ChattyClient)

I learned about most of the Java techniques and APIs used in this during
development, so many things won't be designed ideally. I also never
released such a project as opensource before, so if I missed anything or
didn't adhere to some license correctly, please tell me.

External Libraries/Resources
----------------------------

* JSON-Simple [lib/json-simple-*.jar]
	Website: https://code.google.com/p/json-simple/)
	License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or http://www.apache.org/licenses/LICENSE-2.0).

* JIntellitype [lib/jintellitype-*.jar, Jintellitype*.dll]
	Website: https://code.google.com/p/jintellitype/
	License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or http://www.apache.org/licenses/LICENSE-2.0).

* Favorites Icon [star.png] by Everaldo Coelho
	Source: https://www.iconfinder.com/icons/17999/bookmark_favorite_star_icon 
	License: LGPL
	(for the license text see the LGPL file or
	http://www.gnu.org/licenses/lgpl.html)

* Misc Icons [list-add.png, list-remove.png, view-refresh.png,
		help-browser.png, preferences-system.png,
		dialog-warning.png, go-down.png, go-up.png,
		go-next.png, go-previous.png, go-home.png] from the Tango Icon Theme
	Source: http://tango.freedesktop.org/Tango_Icon_Library
	License: Released into the Public Domain

* Edit Icon [edit.png] from NUVOLA ICON THEME for KDE 3.x by David Vignoni
	Source: http://www.icon-king.com/projects/nuvola/
	License: LGPL
	(for the license text see the LGPL file or
	http://www.gnu.org/licenses/lgpl.html)


Chatty
------

This application (except the parts mentioned above) is released under the
MIT License.

Copyright (c) 2014 tduva

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.


Notes on building the program yourself
--------------------------------------

Just create your own Ant/Maven files to build the project or import the sources
into an IDE. Add the libraries from the /lib folder to your project (as needed).

If you have Hotkey Support enabled (Windows only), you need to include the
JIntellitype32.dll or the JIntellitype64.dll for the 32/64bit versions of Java
respectively (but always renamed to JIntellitype.dll).

In Chatty.java you should set your own client id which you get from Twitch. You
may also want to disable the Version Checker depending on how you will distribute
the compiled program. See the comments in Chatty.java for more information.
