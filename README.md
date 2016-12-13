# DragVideo
A Method to Drag the Video When Playing Video

一种在在播放视频时，能够拖拽的方式

效果图：

![DragVideo](/image/dragvideo.gif)

实现思路：
1、播放视频的view选择TextureView
2、ListView下方盖上自定义ViewDragHelper，当在播放视频时，通过自定义ViewDragHelper进行拖动TextureView
3、进行渐变处理，让两个view的文字能够交替显示
4、当TextureView到达右下方时，控制在水平方向上拖动，到达左边界时，如果再滑动，就销毁TextureView


鸣谢：
@flavienlaurent（法国Fourmob公司工程师）的项目、@Minsoo, Kim（韩国Kakao Corp工程师）的思路

