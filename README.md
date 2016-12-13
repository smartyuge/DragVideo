# DragVideo
A Method to Drag the Video When Playing Video

一种在在播放视频时，能够拖拽的方式

##为什么有这个工程

经常在爱奇艺网站上看电影，看到如果滑动掩盖了播放窗口后，就后在最下面有一个小播放界面。并且这个播放界面，是可以任意拖拽的。感觉很酷

![DragVideoWeb](/image/web_dragvideo.png)

既然web端能实现，就想了想在移动端设备上，是否也能实现这个效果，于是就有了...

##效果图：

![DragVideo](/image/dragvideo.gif)

##实现思路：

- 1、播放视频的view选择TextureView
- 2、ListView下方盖上自定义ViewDragHelper，当在播放视频时，通过自定义ViewDragHelper进行拖动TextureView
- 3、进行渐变处理，让两个view的文字能够交替显示
- 4、当TextureView到达右下方时，控制在水平方向上拖动，到达左边界时，如果再滑动，就销毁TextureView


###特别鸣谢：
@flavienlaurent（法国Fourmob公司工程师）的项目、@Minsoo, Kim（韩国Kakao Corp工程师）的思路, JOJO(阿里巴巴工程师)

