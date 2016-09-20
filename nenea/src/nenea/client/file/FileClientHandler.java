/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package nenea.client.file;

import java.io.File;
import java.io.FileOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Handles a client-side channel.
 */
@Sharable
public class FileClientHandler extends SimpleChannelInboundHandler<Object> {

	int fileNameSize;
	String fileName, localFileName;
	long fileSize;
	boolean fileHeaderRead = false;
	private long offset = 0;
	long startTm;

	File recFile;
	FileOutputStream fileOutputStream;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client " + ctx.channel().remoteAddress() + " channelActive!");
		// ctx.writeAndFlush("D:\\sample.zip\n");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("channelRead0");
		ByteBuf buf = (ByteBuf) msg;
		System.out.println(buf.readableBytes());
		System.out.println(msg);
		System.out.println(buf.toString(CharsetUtil.UTF_8));
		System.out.println((ByteBufUtil.hexDump(buf)));

		// byte[] msgg = buf.array();
		// System.out.println(buf.readInt());

		int header1 = buf.readInt();
		String header2 = buf.readBytes(header1).toString(CharsetUtil.UTF_8);
		long header3 = buf.readLong();

		System.err.println(header1);
		System.err.println(header2);
		System.err.println(header3);

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		ByteBuf buf = (ByteBuf) msg;
		// System.out.println((ByteBufUtil.hexDump(buf)));

		// 파일 헤더 읽기 처리
		if (!fileHeaderRead) {

			startTm = System.currentTimeMillis();

			/*
			 * if (buf.readableBytes() <= 20) {
			 * System.out.println("buf.readableBytes() : " + buf.readableBytes()
			 * + "읽을 바이트가 20도 안됩니다. "); return; }
			 */

			fileNameSize = buf.readInt();
			if (fileNameSize >= 100 || fileNameSize < 0) {
				System.out.println("fileNameSize : " + fileNameSize);
				return;
			}

			fileName = buf.readBytes(fileNameSize).toString(CharsetUtil.UTF_8);
			fileSize = buf.readLong();
			System.out.println("fileSize : " + fileSize + ", fileName : " + fileName + ", fileNameSize" + fileNameSize);

			fileHeaderRead = true;
			localFileName = "D:\\nene_data\\" + System.currentTimeMillis() + fileName;
			recFile = new File(localFileName);
			fileOutputStream = new FileOutputStream(recFile);
		}

		int a = buf.readableBytes();
		int b = (int) Math.min(fileSize - offset, a);
		// System.out.println("### readableBytes : " + a);
		// System.out.println("### to read bytes : " + b);
		// System.out.println("fileSize - offset : " + (fileSize - offset));

		buf.readBytes(fileOutputStream, b);

		offset += b;
		// System.out.println("offset : " + offset);

		// 파일사이즈만큼 다 읽었으면 초기화
		if (offset >= fileSize) {
			offset = 0;
			fileHeaderRead = false;
			if (fileOutputStream != null)
				fileOutputStream.close();
			System.out.println("파일저장완료 : " + localFileName);
			System.out.println("파일저장처리시간 : " + ((System.currentTimeMillis() - startTm) / 1000.0f) + "초");
			return;
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
